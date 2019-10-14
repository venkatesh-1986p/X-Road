/**
 * The MIT License
 * Copyright (c) 2018 Estonian Information System Authority (RIA),
 * Nordic Institute for Interoperability Solutions (NIIS), Population Register Centre (VRK)
 * Copyright (c) 2015-2017 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.proxy;


import ee.ria.xroad.common.conf.serverconf.ServerConf;
import ee.ria.xroad.common.conf.serverconf.model.DescriptionType;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.ServiceId;
import ee.ria.xroad.common.message.RestMessage;
import ee.ria.xroad.common.util.MimeUtils;
import ee.ria.xroad.proxy.testutil.TestServerConf;
import ee.ria.xroad.proxy.testutil.TestService;

import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Test;

import javax.servlet.ServletOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * RestProxyTest
 */
@Slf4j
public class RestProxyTest extends AbstractProxyIntegrationTest {

    static final String PREFIX = "/r" + RestMessage.PROTOCOL_VERSION;

    @Test
    public void shouldFailIfClientHeaderMissing() throws IOException {
        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .get(PREFIX + "/EE/BUSINESS/producer/sub/echo")
                .then()
                .statusCode(Matchers.is(500))
                .header("X-Road-Error", Matchers.notNullValue());
    }

    @Test
    public void shouldFailIfMalformedRequestURI() throws IOException {
        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .get(PREFIX + "/invalid")
                .then()
                .statusCode(Matchers.is(500))
                .header("X-Road-Error", Matchers.notNullValue());
    }

    @Test
    public void shouldHandleSimplePost() throws IOException {
        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .body("{\"value\" : 42}")
                .post(PREFIX + "/EE/BUSINESS/producer/sub/echo")
                .then()
                .statusCode(200)
                .body("value", Matchers.equalTo(42));
    }

    @Test
    public void shouldKeepQueryId() {
        final String qid = UUID.randomUUID().toString();
        service.setHandler((target, request, response) -> assertEquals(qid, request.getHeader("X-Road-Id")));

        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .header("X-Road-Id", qid)
                .body("{\"value\" : 42}")
                .post(PREFIX + "/EE/BUSINESS/producer/sub/echo")
                .then()
                .statusCode(200)
                .header("X-Road-Id", qid);

    }

    @Test
    public void requestHashShouldBeUnique() {
        final String qid = "queryid";
        final String requestHash = given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .header("X-Road-Id", qid)
                .post(PREFIX + "/EE/BUSINESS/producer/sub/echo")
                .header(MimeUtils.HEADER_REQUEST_HASH);

        assertNotNull(requestHash);

        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .header("X-Road-Id", qid)
                .post(PREFIX + "/EE/BUSINESS/producer/sub/echo")
                .then()
                .header(MimeUtils.HEADER_REQUEST_HASH, Matchers.not(requestHash));
    }

    @Test
    public void shouldHaveOnlyOneDateHeader() throws IOException {
        assertEquals(1, given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .body("{\"value\" : 42}")
                .post(PREFIX + "/EE/BUSINESS/producer/sub/echo")
                .headers().getValues("Date").size());
    }

    @Test
    public void shouldAcceptPercentEncodedIdentifiers() throws IOException {
        service.setHandler((target, request, response) -> assertEquals("/path%3B/", request.getRequestURI()));
        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub%2Fsystem")
                .urlEncodingEnabled(false)
                .get(PREFIX + "/EE/BUSINESS/producer/s%2Fub/%C3%B6%C3%A4%C3%A5/path%3B/")
                .then().statusCode(200);
    }

    @Test
    public void shouldAcceptEmptyPath() throws IOException {
        service.setHandler((target, request, response) -> assertEquals("/", request.getRequestURI()));
        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("X-Road-Client", "EE/BUSINESS/consumer/subsystem")
                .get(PREFIX + "/EE/BUSINESS/producer/sub/service/");
    }

    @Test
    public void shouldGetLargeBinaryMessage() throws Exception {
        service.setHandler(LARGE_OBJECT_HANDLER);
        final int requestedBytes = 13 * 1024 * 1024 + 104729;

        final InputStream stream = given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("Content-Type", "application/json")
                .header("X-Road-Client", "EE/BUSINESS/consumer/sub")
                .queryParam("bytes", requestedBytes)
                .get(PREFIX + "/EE/BUSINESS/producer/sub/test").asInputStream();

        long c = 0;
        int r;
        byte[] buf = new byte[8192];
        while ((r = stream.read(buf)) >= 0) {
            c += r;
        }
        assertEquals(requestedBytes, c);
        stream.close();
    }

    @Test
    public void shouldNotFollow302Redirects() {
        final String location = PREFIX + "/EE/BUSINESS/producer/sub/notexists";
        service.setHandler((target, request, response) -> {
            response.setStatus(302);
            response.setHeader("Location", location);
        });
        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("X-Road-Client", "EE/BUSINESS/consumer/subsystem")
                .redirects().follow(false)
                .get(PREFIX + "/EE/BUSINESS/producer/sub/service")
                .then()
                .statusCode(302)
                .header("Location", location);
    }

    @Test
    public void shouldNotAllowCallingWSDLServices() {
        ServerConf.reload(new TestServerConf(servicePort) {
            @Override
            public DescriptionType getDescriptionType(ServiceId service) {
                if ("wsdl".equals(service.getServiceCode())) {
                    return DescriptionType.WSDL;
                }
                return DescriptionType.REST;
            }
        });

        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("X-Road-Client", "EE/BUSINESS/consumer/subsystem")
                .get(PREFIX + "/EE/BUSINESS/producer/sub/wsdl")
                .then()
                .statusCode(500)
                .header("X-Road-Error", "Server.ServerProxy.ServiceType");
    }

    @Test
    public void shouldNotAllowPATCH() {
        ServerConf.reload(new TestServerConf(servicePort) {
            @Override
            public boolean isQueryAllowed(ClientId sender, ServiceId service, String method, String path) {
                if ("PATCH".equalsIgnoreCase(method)) return false;
                return super.isQueryAllowed(sender, service, method, path);
            }
        });

        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("X-Road-Client", "EE/BUSINESS/consumer/subsystem")
                .get(PREFIX + "/EE/BUSINESS/producer/sub/service")
                .then()
                .statusCode(200);

        given()
                .baseUri("http://127.0.0.1")
                .port(proxyClientPort)
                .header("X-Road-Client", "EE/BUSINESS/consumer/subsystem")
                .patch(PREFIX + "/EE/BUSINESS/producer/sub/service")
                .then()
                .statusCode(500)
                .header("X-Road-Error", "Server.ServerProxy.AccessDenied");
    }


    private static final TestService.Handler LARGE_OBJECT_HANDLER = (target, request, response) -> {
        response.setStatus(200);
        response.setContentType("application/octet-stream");
        final int bytes = Integer.valueOf(request.getParameter("bytes"));
        final ServletOutputStream output = response.getOutputStream();
        byte[] buf = new byte[8192];
        for (int i = 0; i < buf.length; i++) {
            buf[i] = (byte)(bytes % 256);
        }
        int i = bytes;
        for (; i > buf.length; i -= buf.length) {
            output.write(buf);
        }
        output.write(buf, 0, i);
        output.close();
    };
}
