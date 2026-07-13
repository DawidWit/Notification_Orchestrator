CREATE TABLE delivery_record (
    id             BIGINT        IDENTITY(1,1) NOT NULL,
    event_id       VARCHAR(64)   NOT NULL,
    user_id        VARCHAR(64)   NOT NULL,
    event_type     VARCHAR(128)  NOT NULL,
    channel        VARCHAR(16)   NOT NULL,
    status         VARCHAR(16)   NOT NULL,
    attempt_count  INT           NOT NULL,
    failure_reason VARCHAR(512)  NULL,
    created_at     DATETIME2     NOT NULL,
    updated_at     DATETIME2     NOT NULL,
    CONSTRAINT pk_delivery_record PRIMARY KEY (id),
    CONSTRAINT uq_delivery_record_event_channel UNIQUE (event_id, channel)
);
