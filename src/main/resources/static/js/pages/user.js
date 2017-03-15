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

    $.ajax({
        url: '/private/api/v1/views/count',
        type: 'GET',
        contentType: 'application/json',
        success: function (data) {
            $('#UserCount').text("User Count: " + data);
        },
        error: function (e) {
            console.log(e.message())
        }
    });
});