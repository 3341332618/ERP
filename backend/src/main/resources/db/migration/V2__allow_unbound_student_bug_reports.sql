ALTER TABLE test_bug_report
    DROP FOREIGN KEY fk_report_bug;

ALTER TABLE test_bug_report
    MODIFY bug_id VARCHAR(64) NULL COMMENT '缺陷定义编码，学员自发现报告可为空';

ALTER TABLE test_bug_report
    ADD CONSTRAINT fk_report_bug FOREIGN KEY (bug_id) REFERENCES test_bug_definition (id);
