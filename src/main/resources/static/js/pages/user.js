var eventbus = new EventBus("/eventbus");
eventbus.onopen = function () {
    eventbus.registerHandler("messenger", function (err, msg) {
        console.log(msg);
        Materialize.toast(msg.headers.name + ": " +  msg.body, 2500);
    });
    var sendMessage = function () {
        var input = $("#MessageInput").val();
        eventbus.publish("messenger", safe_tags_replace(input));
    };
    $("#MessageInput").keyup(function (e) {
        if (e.keyCode === 13) {
            sendMessage();
        }
    });
    $("#SendMessage").click(function () {
        sendMessage();
    });

    var initMap = function() {
        var liivi2 = {lat: 58.378367, lng: 26.714695};
        var map = new google.maps.Map(document.getElementById('map'), {
            zoom: 18,
            center: liivi2
        });
        var marker = new google.maps.Marker({
            position: liivi2,
            map: map
        });
    }

    //$("#map"). = initMap();
    $(document).ready(function () {
        initMap();
    })
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



