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
package ee.ria.xroad.proxy.messagelog;

import ee.ria.xroad.common.messagelog.MessageLogProperties;

import akka.actor.UntypedActor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.query.Query;
import org.joda.time.DateTime;

import static ee.ria.xroad.proxy.messagelog.MessageLogDatabaseCtx.doInTransaction;


/**
 * Deletes all archived log records from the database.
 */
@Slf4j
public class LogCleaner extends UntypedActor {

    public static final String START_CLEANING = "doClean";
    public static final int CLEAN_BATCH_LIMIT = MessageLogProperties.getCleanTransactionBatchSize();

    @Override
    public void onReceive(Object message) {
        log.trace("onReceive({})", message);

        if (message.equals(START_CLEANING)) {
            try {
                log.info("Removing archived records from database...");
                final long removed = handleClean();
                if (removed == 0) {
                    log.info("No archived records to remove from database");
                } else {
                    log.info("Removed {} archived records from database", removed);
                }
            } catch (Exception e) {
                log.error("Error when cleaning archived records from database", e);
            }
        } else {
            unhandled(message);
        }
    }

    protected long handleClean() throws Exception {

        final Long time = new DateTime().minusDays(MessageLogProperties.getKeepRecordsForDays()).getMillis();
        long count = 0;
        int removed;
        do {
            removed = doInTransaction(session -> {
                final Query query = session.getNamedQuery("delete-logrecords");
                query.setParameter("time", time);
                query.setParameter("limit", CLEAN_BATCH_LIMIT);
                return query.executeUpdate();
            });
            log.debug("Removed {} archived records", removed);
            count += removed;
        } while (removed > 0);
        return count;
    }
}
