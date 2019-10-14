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
package ee.ria.xroad.centerui;

import ee.ria.xroad.common.SystemPropertiesLoader;
import ee.ria.xroad.commonui.UIServices;
import ee.ria.xroad.signer.protocol.SignerClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static ee.ria.xroad.common.SystemProperties.CONF_FILE_CENTER;
import static ee.ria.xroad.common.SystemProperties.CONF_FILE_SIGNER;

/**
 * Contains the UI actor system instance.
 */
public final class CenterUIServices {

    private static final Logger LOG =
            LoggerFactory.getLogger(CenterUIServices.class);

    static {
        SystemPropertiesLoader.create().withCommonAndLocal()
                .with(CONF_FILE_CENTER)
                .with(CONF_FILE_SIGNER)
                .load();
    }

    private static UIServices instance;

    private CenterUIServices() {
    }

    private static void init() throws Exception {
        if (instance == null) {
            instance = new UIServices("CenterUI", "centerui");
        }
    }

    /**
     * Initializes the UI actor system.
     * @throws Exception in case of any errors
     */
    public static void start() throws Exception {
        LOG.info("start()");

        init();

        SignerClient.init(instance.getActorSystem());
    }

    /**
     * Stops the UI actor system.
     * @throws Exception in case of any errors
     */
    public static void stop() throws Exception {
        instance.stop();
    }
}
