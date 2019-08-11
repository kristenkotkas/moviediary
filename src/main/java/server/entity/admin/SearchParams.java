package server.entity.admin;

import io.vertx.rxjava.core.MultiMap;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class SearchParams {

    protected final MultiMap params;
    protected List<String> invalidParameters = new ArrayList<>();

    protected SearchParams(MultiMap params) {
        this.params = params;
    }

    protected abstract boolean isParamsNotPresent();

    public void checkParameters() throws BadRequestException {
        if (!invalidParameters.isEmpty()) {
            throw new BadRequestException("Invalid value for parameter: " + invalidParameters.toString());
        }

        if (isParamsNotPresent()) {
            throw new BadRequestException("At least 1 query parameter must exist.");
        }
    }

    protected <T> void setValue(Consumer<T> setter, Function<String, T> valueGetter, String param, String regex) {
        if (isValid(param, "^" + regex + "$")) {
            setter.accept(valueGetter.apply(getParamValue(param)));
        }
    }

    private String getParamValue(String param) {
        return params.get(param);
    }

    private boolean isValid(String param, String regex) {
        String value = getParamValue(param);
        if (value == null) {
            return false;
        }
        if (!value.matches(regex)) {
            invalidParameters.add(param + "=" + value);
            return false;
        }
        return true;
    }
}
