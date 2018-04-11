var navbarButton = $("#navbar-history");
navbarButton.addClass('navbar-text-active');
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
    $('.collapsible').collapsible();
});

var eventbus = new EventBus("/eventbus");
var startDateField = $("#startingDay");
var endDateField = $("#endDay");
var yearDropdown = $("#history-year-drop");
var collSearch = $("#history-coll-search");
var collFilters = $("#history-coll-filters");
var collQkSearch = $("#history-coll-qk-search");
var search = $("#search");
var today = $("#today");
var thisWeek = $("#this-week");
var thisMonth = $("#this-month");
var allTime = $("#all-time");
var monthBack = $("#month-back-history");
var monthNext = $("#month-next-history");
var monthIndex = 0;
eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {

        if ($( window ).width() > 600) {
            openCollapsible();
        }

        lang = reply.body;
        fillDropDown(lang);
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
            var dates = getThisMonth(0);
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });

        allTime.click(function () {
            makeAllTime(eventbus, lang);
        });

        monthBack.click(function () {
            var dates = getThisMonth(--monthIndex);
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });

        monthNext.click(function () {
            var dates = getThisMonth(++monthIndex);
            startDateField.pickadate('picker').set('select', dates['start']);
            endDateField.pickadate('picker').set('select', dates['end']);
            makeHistory(eventbus, lang, startDateField.val(), endDateField.val());
        });

        searchYear(new Date().getFullYear(), lang, startDateField, endDateField);
    });
};

var openCollapsible = function () {
    collFilters.collapsible('open', 0);
    collQkSearch.collapsible('open', 0);
    collSearch.collapsible('open', 0);
/*
    collFilters.collapsible({
        accordion: false,
        onOpen: function (el) {
            console.log('filter open');
        }
    });*/
};

var fillDropDown = function (lang) {
    eventbus.send("database_get_all_time_meta",
        {
            'is-first': $("#seenFirst").is(':checked'),
            'is-cinema': $("#wasCinema").is(':checked')
        }
        , function (error, reply) {
            var data = reply.body['rows'];
            console.log(data);
            var start = new Date(data[0]['Start']).getFullYear();
            if (data[0]['Start'] != null) {
                makeDropList(start, lang, 'history', startDateField, endDateField);
            }
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
    eventbus.send("database_get_all_time_meta",
        {
            'is-first': $("#seenFirst").is(':checked'),
            'is-cinema': $("#wasCinema").is(':checked')
        }
        , function (error, reply) {
            var data = reply.body['rows'];
            //console.log(data);
            //console.log(new Date(data[0]['Start']));
            startDateField.pickadate('picker').set('select', new Date(data[0]['Start']));
            endDateField.pickadate('picker').set('select', new Date());
            searchHistory(eventbus, lang, startDateField.val(), endDateField.val(), data[0]['Count']);
        });
};

var searchHistory = function (eventbus, lang, start, end, count) {
    eventbus.send("database_get_history",
        {
            'is-first': $("#seenFirst").is(':checked'),
            'is-cinema': $("#wasCinema").is(':checked'),
            'start': start,
            'end': end,
            'page': 0
        }, function (error, reply) {
            //console.log(reply);
            var data = reply.body['rows'];
            //console.log(data.length);
            if (data.length > 0) {
                $("#viewsTitle").empty();
                addTableHead(count, lang);
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
                addNotFound(lang);
            }
        });
};

function addTableHead(data, lang) {
    var views = '';
    if (data > 0) {
        if (data === 1) {
            views = lang['HISTORY_VIEW'];
        } else {
            views = lang['HISTORY_VIEWS'];
        }
        $("#viewsTitle").empty().append(
            '<div class="card z-depth-0">' +
            '<div class="card-title">' +
            '<span class="light grey-text text-lighten-1 not-found">' + data + ' ' + views + '</span><br>' +
            //'<span class="grey-text not-found-info">' + date + '</span>' +
            '</div>' +
            '</div>'
        );
    } else {
        addNotFound(lang);
    }
}

function addNotFound(lang) {
    $("#viewsTitle").empty().append(
        '<div class="card z-depth-0">' +
        '<div class="card-title">' +
        '<span class="light grey-text text-lighten-1 not-found">' + lang['HISTORY_NOT_PRESENT'] + '</span><br>' +
        //'<span class="grey-text not-found-info">' + date + '</span>' +
        '</span>' +
        '</div>' +
        '</div>'
    );
}

function removeView(viewId, lang) {
    console.log(viewId);
    eventbus.send("database_remove_view", viewId, function (error, reply) {
        console.log(viewId + ' removed');
        document.getElementById('history-' + viewId).remove();
        eventbus.send("database_get_history_meta",
            {
                'is-first': $("#seenFirst").is(':checked'),
                'is-cinema': $("#wasCinema").is(':checked'),
                'start': $("#startingDay").val(),
                'end': $("#endDay").val()
            }
            , function (error, reply) {
                var data = reply.body['rows'];
                addTableHead(data[0]['Count'], lang);
            });
    });
}

function addHistory(data, lang) {
    $.each(data, function (i) {
        var posterPath = "";
        if (data[i]['Image'] != "") {
            posterPath = 'https://image.tmdb.org/t/p/w342' + data[i]['Image'];
        } else {
            posterPath = '/static/img/nanPosterBig.jpg';
        }
        //console.log(data[i]);
        $("#table").append(
            $.parseHTML(
                '<li class="z-depth-0" id="history-' + data[i]['Id'] + '">' +
                    '<div class="collapsible-header collapsible-header-history history-object content-key grey-text" id="header_' + data[i]['Id'] + '">' +
                        '<span><i class="fa fa-angle-down grey-text" aria-hidden="true" id="arrow_' + data[i]['Id'] + '"></i>' + data[i]['Title'] + '</span>'+
                        '<span class="hide-on-small-only badge ' + data[i]['WasCinema'] + '" aria-hidden="true"></span>' +
                        '<span class="badge new ">' + getMonth(data[i]['Start'], lang) + '</span>' +
                    '</div>' +
                    //body starts here
                    '<div class="collapsible-body white">' +
                        '<div class="row">' +
                            '<div class="col m4 l3 search-image hide-on-small-only">' +
                                '<div class="row">' +
                                    '<a class="wishlist-object" href="movies/?id=' + data[i]['MovieId'] + '">' +
                                        '<img class="wishlist-object img-80" src="' + posterPath  + '" alt="Poster for movie: ' + data[i]['Title'] + '">' +
                                    '</a>' +
                                '</div>' +
                                '<div class="row">' +
                                    '<a class="waves-effect waves-light btn red z-depth-0" id="' + data[i]['Id'] +'">' + lang['HISTORY_REMOVE'] + '</a>' +
                                '</div>' +
                            '</div>' +
                            '<div class="col m8 l9">' +
                                '<ul>' +
                                    '<li>' +
                                        '<table>' +
                                            '<tbody>' +
                                                '<tr>' +
                                                    '<td class="col s12 m10 l10">' +
                                                    '<span class="grey-text history-date">' + getMonth(data[i]['Start'], lang) + '</span><br>' +
                                                    '<span class="content-key grey-text">' + data[i]['Time'] + '</span>' +
                                                    '</td>' +
                                                    '<td class="col s12 m2 l2"><span class="content-key grey-text">' + lang[data[i]['DayOfWeek']] + '</span></td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td class="col s12 m10 l10"><span class="content-key grey-text">' + minutesToString(data[i]['Runtime'], lang) + '</span></td>' +
                                                    '<td></td>' +
                                                '</tr>' +
                                                '<tr>' +
                                                    '<td class="col">' +
                                                        '<i class="fa-lg grey-text ' + data[i]['WasFirst'] + '" aria-hidden="true"></i>' +
                                                        '&nbsp&nbsp' +
                                                        '<i class="fa-lg grey-text ' + data[i]['WasCinema'] + '" aria-hidden="true"></i>' +
                                                    '</td>' +
                                                    '<td></td>' +
                                                '</tr>' +
                                            '</tbody>' +
                                        '</table>' +
                                    '</li>' +
                                    '<li>' +
                                        '<div>' +
                                            '<span class="custom-truncate">' +
                                                safe_tags_replace(data[i]['Comment']) +
                                            '</span>' +
                                        '</div>' +
                                    '</li>' +
                                '</ul>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</li>'
            )
        );

        var button = document.getElementById(data[i]['Id']);
        button.onclick = function () {
            removeView(data[i]['Id'], lang);
        };

        $(document.getElementById('table')).collapsible({
            accordion: false, // A setting that changes the collapsible behavior to expandable instead of the default accordion style
            onOpen: function (el) {
                $(el.find('i.fa')[0]).removeClass('fa-angle-down').addClass('fa-angle-up');
            },
            onClose: function (el) {
                $(el.find('i.fa')[0]).removeClass('fa-angle-up').addClass('fa-angle-down');
            }
        });

    });
}