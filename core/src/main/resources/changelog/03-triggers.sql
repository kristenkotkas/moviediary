CREATE TRIGGER user_account_before_insert
    BEFORE INSERT
    ON user_account
    FOR EACH ROW
EXECUTE PROCEDURE add_required_values();