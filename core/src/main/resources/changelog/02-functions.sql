CREATE OR REPLACE FUNCTION get_skey()
    RETURNS varchar(32) AS
$$
BEGIN
    RETURN substr(encode(sha224(gen_random_bytes(64)), 'hex'), 32);
END;
$$
    LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION add_required_values()
    RETURNS trigger AS
$$
BEGIN
    new.skey := (select * from get_skey());
    new.time_added := current_timestamp;
    return new;
END;
$$
    LANGUAGE plpgsql VOLATILE;