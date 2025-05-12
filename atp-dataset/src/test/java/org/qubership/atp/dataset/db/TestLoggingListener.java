/*
 * # Copyright 2024-2025 NetCracker Technology Corporation
 * #
 * # Licensed under the Apache License, Version 2.0 (the "License");
 * # you may not use this file except in compliance with the License.
 * # You may obtain a copy of the License at
 * #
 * #      http://www.apache.org/licenses/LICENSE-2.0
 * #
 * # Unless required by applicable law or agreed to in writing, software
 * # distributed under the License is distributed on an "AS IS" BASIS,
 * # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * # See the License for the specific language governing permissions and
 * # limitations under the License.
 */

package org.qubership.atp.dataset.db;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.querydsl.core.QueryMetadata;
import com.querydsl.core.types.Expression;
import com.querydsl.core.types.Path;
import com.querydsl.core.types.SubQueryExpression;
import com.querydsl.sql.RelationalPath;
import com.querydsl.sql.SQLDetailedListener;
import com.querydsl.sql.SQLListenerContext;
import com.querydsl.sql.dml.SQLInsertBatch;
import com.querydsl.sql.dml.SQLMergeBatch;
import com.querydsl.sql.dml.SQLUpdateBatch;

public class TestLoggingListener implements SQLDetailedListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestLoggingListener.class);

    @Override
    public void start(SQLListenerContext context) {
        LOGGER.trace("start {}", context);
    }

    @Override
    public void preRender(SQLListenerContext context) {
        LOGGER.trace("preRender {}", context);
    }

    @Override
    public void rendered(SQLListenerContext context) {
        LOGGER.trace("rendered {}", context);
    }

    @Override
    public void prePrepare(SQLListenerContext context) {
        LOGGER.trace("prePrepare {}", context);
    }

    @Override
    public void prepared(SQLListenerContext context) {
        LOGGER.trace("prepared {}", context);
    }

    @Override
    public void preExecute(SQLListenerContext context) {
        LOGGER.trace("preExecute {}", context);
    }

    @Override
    public void executed(SQLListenerContext context) {
        LOGGER.trace("executed {}", context);
    }

    @Override
    public void exception(SQLListenerContext context) {
        LOGGER.trace("exception {}", context);
    }

    @Override
    public void end(SQLListenerContext context) {
        LOGGER.trace("end {}", context);
    }

    @Override
    public void notifyQuery(QueryMetadata md) {
        LOGGER.trace("notifyQuery {}", md);
    }

    @Override
    public void notifyDelete(RelationalPath<?> entity, QueryMetadata md) {
        LOGGER.trace("notifyDelete {}", entity);
    }

    @Override
    public void notifyDeletes(RelationalPath<?> entity, List<QueryMetadata> batches) {
        LOGGER.trace("notifyDeletes {}", entity);
    }

    @Override
    public void notifyMerge(RelationalPath<?> entity, QueryMetadata md, List<Path<?>> keys, List<Path<?>> columns, List<Expression<?>> values, SubQueryExpression<?> subQuery) {
        LOGGER.trace("notifyMerge {}", entity);
    }

    @Override
    public void notifyMerges(RelationalPath<?> entity, QueryMetadata md, List<SQLMergeBatch> batches) {
        LOGGER.trace("notifyMerges {}", entity);
    }

    @Override
    public void notifyInsert(RelationalPath<?> entity, QueryMetadata md, List<Path<?>> columns, List<Expression<?>> values, SubQueryExpression<?> subQuery) {
        LOGGER.trace("notifyInsert {}", entity);
    }

    @Override
    public void notifyInserts(RelationalPath<?> entity, QueryMetadata md, List<SQLInsertBatch> batches) {
        LOGGER.trace("notifyInserts {}", entity);
    }

    @Override
    public void notifyUpdate(RelationalPath<?> entity, QueryMetadata md, Map<Path<?>, Expression<?>> updates) {
        LOGGER.trace("notifyUpdate {}", entity);
    }

    @Override
    public void notifyUpdates(RelationalPath<?> entity, List<SQLUpdateBatch> batches) {
        LOGGER.trace("notifyUpdates {}", entity);
    }
}

