DELIMITER //

CREATE TRIGGER trg_responses_bi BEFORE INSERT ON responses FOR EACH ROW
BEGIN
  DECLARE anon BOOLEAN;
  SELECT is_anonymous INTO anon FROM surveys WHERE survey_id = NEW.survey_id;
  IF anon THEN
    IF NEW.student_id IS NOT NULL OR NEW.submitted_at IS NOT NULL OR NEW.group_section IS NOT NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Anonymous survey: identity, time and section must be NULL';
    END IF;
  ELSE
    IF NEW.student_id IS NULL THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Non-anonymous survey requires student_id';
    END IF;
  END IF;
END//

CREATE TRIGGER trg_responses_bu BEFORE UPDATE ON responses FOR EACH ROW
BEGIN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Responses are immutable';
END//

CREATE TRIGGER trg_surveys_bi BEFORE INSERT ON surveys FOR EACH ROW
BEGIN
  IF NEW.category = 'WELLBEING' AND NEW.is_anonymous = FALSE THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Wellbeing surveys must be anonymous';
  END IF;
END//

CREATE TRIGGER trg_surveys_bu BEFORE UPDATE ON surveys FOR EACH ROW
BEGIN
  IF OLD.status <> 'DRAFT' THEN
    IF NEW.is_anonymous <> OLD.is_anonymous THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Anonymity cannot change after activation';
    END IF;
    IF NOT (NEW.question_schema <=> OLD.question_schema) THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Questions cannot change after activation';
    END IF;
  END IF;
  IF NEW.category = 'WELLBEING' AND NEW.is_anonymous = FALSE THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Wellbeing surveys must be anonymous';
  END IF;
END//

DELIMITER ;
