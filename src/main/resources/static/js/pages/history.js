fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("{{eventbusUrl}}");
    eventbus.onopen = function () {
        $("#Search").click(function () {
            eventbus.send("database_get_history",
                {
                    'is-first': $("#seenFirst").is(':checked'),
                    'is-cinema': $("#wasCinema").is(':checked'),
                    'start': $("#startingDay").pickadate('picker').get(),
                    'end': $("#endDay").pickadate('picker').get()
                }, function (error, reply) {
                    console.log(reply);
                    var data = reply.body['rows'];
                    console.log(data.length);
                    if (data.length > 0) {
                        $("#viewsTitle").empty();
                        $("#table").empty().append(
                            '<tr>' +
                            '<th class="table-row">{{i18n "HISTORY_TITLE" locale=lang}}</th>' +
                            '<th>{{i18n "HISTORY_DATE" locale=lang}}</th>' +
                            '<th>{{i18n "HISTORY_TIME" locale=lang}}</th>' +
                            '<th></th>' +
                            '<th></th>' +
                            '<th></th>' +
                            '</tr>');
                        $.each(data, function (i) {
                            $("#table").append(
                                $.parseHTML('<tr>' +
                                    '<td class="table-row">' + data[i].Title + '</td>' +
                                    '<td>' + getMonth(data[i].Start) + '</td>' +
                                    '<td>' + data[i].Time + '</td>' +
                                    '<td>' + data[i].DayOfWeek + '</td>' +
                                    '<td class="center"><i class=' + data[i].WasFirst + ' aria-hidden="true"></i></td>' +
                                    '<td class="center"><i class=' + data[i].WasCinema + ' aria-hidden="true"></i></td>' +
                                    '</tr>')
                            );
                        });
                    } else {
                        $("#table").empty();
                        $("#viewsTitle").empty().append(
                            '<div class="card z-depth-0">' +
                            '<div class="card-title">' +
                            '<a class="light grey-text text-lighten-1 not-found">{{i18n "HISTORY_NOT_PRESENT" locale=lang}}</a>' +
                            '</div>' +
                            '</div>'
                        );
                    }
                });
        });
    };
});

var getMonth = function (start) {
    var startArray = start.split(' ');
    var month = startArray[1];
    switch (month) {
        case 'January':return startArray[0] + "{{i18n "JANUARY" locale=lang}}" + ' ' +  startArray[2];
        case 'February':return startArray[0] + "{{i18n "FEBRUARY" locale=lang}}" + ' ' +  startArray[2];
        case 'March':return startArray[0] + "{{i18n "MARCH" locale=lang}}" + ' ' +  startArray[2];
        case 'April':return startArray[0] + "{{i18n "APRIL" locale=lang}}" + ' ' +  startArray[2];
        case 'May':return startArray[0] + "{{i18n "MAY" locale=lang}}" + ' ' +  startArray[2];
        case 'June':return startArray[0] + "{{i18n "JUNE" locale=lang}}" + ' ' +  startArray[2];
        case 'July':return startArray[0] + "{{i18n "JULY" locale=lang}}" + ' ' +  startArray[2];
        case 'August':return startArray[0] + "{{i18n "AUGUST" locale=lang}}" + ' ' +  startArray[2];
        case 'September':return startArray[0] + "{{i18n "SEPTEMBER" locale=lang}}" + ' ' +  startArray[2];
        case 'October':return startArray[0] + "{{i18n "OCTOBER" locale=lang}}" + ' ' +  startArray[2];
        case 'November':return startArray[0] + "{{i18n "NOVEMBER" locale=lang}}" + ' ' +  startArray[2];
        case 'December':return startArray[0] + "{{i18n "DECEMBER" locale=lang}}" + ' ' +  startArray[2];
    }
}