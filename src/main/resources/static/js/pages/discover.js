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
var recommendations = {};


eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;

        addInput.keyup(function (e) {
            if (e.keyCode === 13) {
                getDiscover();
            }
        });
    });
};

function getDiscover() {
    eventbus.send("api_get_recommendations", addInput.val(), function (error, reply) {
        console.log(reply.body);
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
    discoverTable.empty();

    var json = eval(recommendations);

    Object.keys(json).sort(function (a, b) {
        return (json[b].count.toString()).localeCompare(json[a].count);
    }).forEach(function (key) {
        var movie = json[key];
        discoverTable.append(
            $.parseHTML(
                '<tr>' +
                '<td>' + movie['count'] + '</td>' +
                '<td>' + movie['id'] + '</td>' +
                '<td><img width="100" src="' + getPosterPath(movie['poster']) +'"></td>' +
                '<td>' + movie['title'] + '</td>' +
                '</tr>'
            )
        );
    });
}

function getPosterPath(posterPath) {
    if (posterPath != null) {
        return 'https://image.tmdb.org/t/p/w300' + posterPath;
    } else {
        return '/static/img/nanPosterBig.jpg'
    }
}