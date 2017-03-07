fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        eventbus.registerHandler("messenger", function (err, msg) {
            console.log(msg);
            Materialize.toast(msg.headers.name + ": " + msg.body, 2500);
        });
        var sendMessage = function () {
            var input = $("#MessageInput").val();
            eventbus.publish("messenger", input);
        };
        $("#MessageInput").keyup(function (e) {
            if (e.keyCode == 13) {
                sendMessage();
            }
        });
        $("#SendMessage").click(function () {
            sendMessage();
        });
    };
});