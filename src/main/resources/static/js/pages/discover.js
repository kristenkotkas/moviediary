$(document).ready(function () {
    $('.tooltipped').tooltip({ //tooltips initialization
        delay: 150,
        position: 'top',
        html: true
    });
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});
var eventbus = new EventBus("/eventbus");

var addInput = $("#discover-add");
var discoverTable = $("#discover-body");
var searchResult = $("#discover-search");
var recommendations = {};


eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;

        addInput.keyup(function (e) {
            if (e.keyCode === 13) {
                getMovies();
            }
        });
    });
};

function getMovies() {
    eventbus.send("api_get_search", addInput.val(), function (error, reply) {
        var search = reply.body['results'];
        //console.log(search);
        searchResult.empty();
        $.each(search, function (i) {
            var movie = search[i];
            searchResult.append(
                $.parseHTML(
                    '<tr>' +
                        '<td>' + movie['title'] + '</td>' +
                        '<td>' + getYear(movie['release_date']) + '</td>' +
                        '<td>' +
                            '<a class="btn z-depth-0 waves-effect" onclick="getDiscover(' + movie['id'] + ')">Add</a>' +
                        '</td>' +
                    '</tr>'
                )
            );
        });
    });
}

function getDiscover(movieId) {
    eventbus.send("api_get_recommendations", movieId, function (error, reply) {
        //console.log(reply.body);
        var data = reply.body['results'];
        $.each(data, function (i) {
            var movie = data[i];
            updateRecommendations(movie);
        });
        updateTable();
    });
}

function updateRecommendations(movie) {
    if (recommendations[movie['id']] == null) {
        var movieData = {};
        movieData.count = 1;
        movieData.id = movie['id'];
        movieData.title = movie['title'];
        movieData.poster = movie['poster_path'];
        recommendations[movie['id']] = movieData;
    } else {
        var count = recommendations[movie['id']]['count'];
        recommendations[movie['id']]['count'] = count + 1;
    }
}

function updateTable() {
    var count = 0;
    discoverTable.empty();
    var json = eval(recommendations);
    Object.keys(json).sort(function (a, b) {
        return (json[b].count.toString()).localeCompare(json[a].count);
    }).forEach(function (key) {
        if (count > 9) {
            return;
        }
        var movie = json[key];
        discoverTable.append(
            $.parseHTML(
                '<tr>' +
                '<td>' + movie['count'] + '</td>' +
                '<td>' + movie['id'] + '</td>' +
                '<td><img width="50" src="' + getPosterPath(movie['poster']) +'"></td>' +
                '<td>' + movie['title'] + '</td>' +
                '</tr>'
            )
        );
        count++;
    });
}

function getPosterPath(posterPath) {
    if (posterPath != null) {
        return 'https://image.tmdb.org/t/p/w300' + posterPath;
    } else {
        return '/static/img/nanPosterBig.jpg'
    }
}

function getYear(airDate) {
    if (airDate != null) {
        return (airDate.split('-')[0]);
    } else return '';
}
