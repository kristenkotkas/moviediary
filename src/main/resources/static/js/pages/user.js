$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
    initMap();
});
var eventbus = new EventBus("/eventbus");
eventbus.onopen = function () {
    eventbus.registerHandler("messenger", function (err, msg) {
        console.log(msg);
        Materialize.toast(msg.headers.name + ": " + msg.body, 2500);
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
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;

        var lang;
        $.ajax({
            url: '/private/api/v1/user/info',
            type: 'GET',
            contentType: 'application/xml',
            success: function (data) {
                var $xml = $($.parseXML(data));
                addUserInfo('ID', $xml.find('id').text());
                addUserInfo(lang['USER_FIRSTNAME'], $xml.find('firstname').text());
                addUserInfo(lang['USER_LASTNAME'], $xml.find('lastname').text());
                addUserInfo(lang['USER_USERNAME'], $xml.find('username').text());
                addUserInfo(lang['USER_RUNTIME_TYPE'], $xml.find('runtimeType').text());
                addUserInfo(lang['USER_VERIFIED'], $xml.find('verified').text());
            },
            error: function (e) {
                console.log(e.message());
                Materialize.toast("Failed to query user data.", 2000);
            }
        });
    });
};

var initMap = function () {
    var liivi2 = {
        lat: 58.378367,
        lng: 26.714695
    };
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 18,
        center: liivi2
    });
    var marker = new google.maps.Marker({
        position: liivi2,
        map: map
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

function addUserInfo(key, value) {
    $("#my-info").append(
        '<tr>' +
        '<td class="grey-text">' + key + '</td>' +
        '<td class="content-key grey-text truncate">' + value + '</td>' +
        '</tr>'
    );
}


