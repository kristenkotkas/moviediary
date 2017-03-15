fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        var lang;
        eventbus.send("translations", getCookie("lang"), function (error, reply) {
            lang = reply.body;
            console.log(lang);
            eventbus.send("database_get_wishlist",
                {}, function (error, reply) {
                    var data = reply.body['rows'];
                    console.log(data);
                    addTableHead(lang);
                    addTableData(data, lang);
                });
        });
    };
});

function addTableHead(lang) {
    $("#wishlist-table").empty().append(
        '<tr>' +
        '<th class="table-row">' + lang['HISTORY_TITLE'] + '</th>' +
        '<th class="center">' + lang['HISTORY_DATE'] + '</th>' +
        '</tr>');
}

function addTableData(data, lang) {
    var timeout = 100;
    $.each(data, function (i) {
        setTimeout(function () {
            $("#wishlist-table").append(
                $.parseHTML('<tr>' +
                    '<td class="table-row">' + data[i]['Title'] + '</td>' +
                    //'<td class="center">' + getMonth(data[i]['Time'], lang) + '</td>' +
                    '<td class="center">' + data[i]['Time'] + '</td>' +
                    '</tr>')
            );
        }, timeout += 25);
    });
}