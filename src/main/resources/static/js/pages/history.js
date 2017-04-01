$(document).ready(function () {
    $(".datepicker").pickadate({ //calendar initialization
        //http://amsul.ca/pickadate.js/date/#options
        selectMonths: true,
        selectYears: 10,
        firstDay: 1
    });
    $('.tooltipped').tooltip({ //tooltips initialization
        delay: 150,
        position: 'top',
        html: true
    });
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});

var eventbus = new EventBus("/eventbus");
var startDateField =  $("#startingDay");
var endDateField = $("#endDay");
eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        console.log(lang);
        var search = $("#search");
        var today = $("#today");
        var thisWeek = $("#this-week");
        var thisMonth = $("#this-month");
        var thisYear = $("#this-year");
        var allTime = $("#all-time");

        search.keyup(function (e) {
            if (e.keyCode === 13) {
                search.click();
            }
        });
        today.keyup(function (e) {
            if (e.keyCode === 13) {
                today.click();
            }
        });
        thisWeek.keyup(function (e) {
            if (e.keyCode === 13) {
                thisWeek.click();
            }
        });
        thisMonth.keyup(function (e) {
            if (e.keyCode === 13) {
                thisMonth.click();
            }
        });
        thisYear.keyup(function (e) {
            if (e.keyCode === 13) {
                thisYear.click();
            }
        });
        allTime.keyup(function (e) {
            if (e.keyCode === 13) {
                allTime.click();
            }
        });
        search.click(function () {
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        today.click(function () {
            startDateField.pickadate('picker').set('select', new Date());
            endDateField.pickadate('picker').set('select', new Date());
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        thisWeek.click(function () {
            var dates = getThisWeek();
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        thisMonth.click(function () {
            var dates = getThisMonth();
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        thisYear.click(function () {
            var dates = getThisYear();
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });
        allTime.click(function () {
            makeAllTime(eventbus, lang);
        });
    });
};

var makeHistory = function (eventbus, lang, start, end) {
    eventbus.send("database_get_history_meta",
        {
            'is-first': $("#seenFirst").is(':checked'),
            'is-cinema': $("#wasCinema").is(':checked'),
            'start': start,
            'end': end
        }
        , function (error, reply) {
            var data = reply.body['rows'];
            searchHistory(eventbus, lang, start, end, data[0]['Count']);
        });
};

var makeAllTime = function (eventbus, lang) {
    eventbus.send("database_get_all_time_meta", {}, function (error, reply) {
        var data = reply.body['rows'];
        startDateField.pickadate('picker').set('select', new Date(data[0]['Start']));
        endDateField.pickadate('picker').set('select', new Date());
        searchHistory(eventbus, lang, startDateField.val(), endDateField.val(), data[0]['Count']);
    });
};

var searchHistory = function(eventbus, lang, start, end, count) {
    eventbus.send("database_get_history",
        {
            'is-first': $("#seenFirst").is(':checked'),
            'is-cinema': $("#wasCinema").is(':checked'),
            'start': start,
            'end': end,
            'page': 0
        }, function (error, reply) {
            console.log(reply);
            var data = reply.body['rows'];
            console.log(data.length);
            if (data.length > 0) {
                $("#viewsTitle").empty();
                addTableHead(count);
                $("#table").empty();
                //addTableHead(data.length);

                addHistory(data, lang);

                if (data.length == 10) {
                    $("#load-more-holder").empty().append(
                        $.parseHTML(
                            '<tr tabindex="8" class="load-more" id="load-more">' +
                            '<td></td>' +
                            '<td>' + lang['HISTORY_LOAD_MORE'] + '</td>' +
                            '</tr>'
                        )
                    ).show();
                } else {
                    $("#load-more-holder").empty();
                }

                var i = 0;

                $("#load-more").keyup(function (e) {
                    if (e.keyCode == 13) {
                        $("#load-more").click();
                    }
                });

                $("#load-more").click(function () {
                    eventbus.send("database_get_history",
                        {
                            'is-first': $("#seenFirst").is(':checked'),
                            'is-cinema': $("#wasCinema").is(':checked'),
                            'start': start,
                            'end': end,
                            'page': ++i
                        }, function (error, reply) {
                            var addData = reply.body['rows'];
                            console.log(addData.length);
                            if (addData.length < 10) {
                                $("#load-more-holder").hide();
                            }
                            addHistory(addData, lang);
                            $(document).scrollTop($(document).height());
                        });
                });

            } else {
                $("#load-more").hide();
                $("#table").empty();
                $("#viewsTitle").empty().append(
                    '<div class="card z-depth-0">' +
                    '<div class="card-title">' +
                    '<a class="light grey-text text-lighten-1 not-found">' + lang['HISTORY_NOT_PRESENT'] + '</a>' +
                    '</div>' +
                    '</div>'
                );
            }
        });
};

function addTableHead(data) {
    $("#viewsTitle").empty().append(
        '<div class="card z-depth-0">' +
        '<div class="card-title">' +
        '<a class="light grey-text text-lighten-1 not-found">' + data + ' views</a>' +
        '</div>' +
        '</div>'
    );
}

function addHistory(data, lang) {
    $.each(data, function (i) {
        var posterPath = "";
        if (data[i]['Image'] != "") {
            posterPath = 'https://image.tmdb.org/t/p/w342' + data[i]['Image'];
        } else {
            posterPath = '/static/img/nanPosterBig.jpg'
        }
        $("#table").append(
            $.parseHTML(
                '<li class="z-depth-0">' +
                    '<div class="collapsible-header collapsible-header-history history-object content-key grey-text">' +
                        data[i]['Title'] +
                        '<span class="hide-on-small-only badge ' + data[i]['WasCinema'] + '" aria-hidden="true"></span>' +
                        '<span class="badge new ">' + getMonth(data[i]['Start'], lang) + '</span>' +
                    '</div>' +
                    //body starts here
                    '<div class="collapsible-body white">' +
                        '<div class="row search-image">' +
                            '<div class="col m4 l3 search-image hide-on-small-only">' +
                                '<a class="wishlist-object" href="movies/?id=' + data[i]['MovieId']  + '">' +
                                    '<img class="wishlist-object" src="' + posterPath + '" alt="Poster for movie: ' +
                                    data[i]['Title'] + '" width="80%">' +
                                '</a>' +
                            '</div>'+
                            '<div class="col m8 l9">' +
                                '<ul>' +
                                    '<li>' +
                                    '<div>' +
                                        getMonth(data[i]['Start'], lang) +
                                        '<span class="content-key grey-text badge">' + data[i]['Time'] + '</span>' +
                                        '<span class="content-key grey-text badge">' + lang[data[i]['DayOfWeek']] + '</span>' +
                                    '</div></li>' +
                                    '<li><i class="grey-text ' + data[i]['WasFirst'] + '" aria-hidden="true"></i></li>' +
                                    '<li><i class="grey-text ' + data[i]['WasCinema'] + '" aria-hidden="true"></i></li>' +
                                    '<li>' +
                                        '<div class="custom-truncate">' +
                                        safe_tags_replace(data[i]['Comment'])+
                                        '</div>' +
                                    '</li>' +
                                '</ul>' +
                            '</div>' +
                            '<div class="row">' +
                                '<div class="col s12 m12 l12">' +
                                '</div>'+
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</li>'
            )
        );
    });
}