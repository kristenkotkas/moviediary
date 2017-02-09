package sys.template.ui;

import io.vertx.core.json.JsonArray;

public interface IndexTemplate extends BaseTemplate {
    IndexTemplate setPealkiri(String pealkiri);

    IndexTemplate setCards(JsonArray cards);

}
