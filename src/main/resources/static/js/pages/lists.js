$("#navbar-lists").addClass('navbar-text-active');
$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
    $('.modal').modal({
        complete: modalClose
    }); //movies modal initialization
});

var eventbus = new EventBus("/eventbus");
var inputNewListName = $('#createListName');
var newListModal = $('#modalList');
var listsTable = $('#lists-tabel');
var listContent = $('#list-content');
var lang;

eventbus.onopen = function () {
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        getLists();
    });
};

function modalClose() {
    console.log('modal closed');
    inputNewListName.val('');
}

function createNewList() {
    if (inputNewListName.val().length > 0 && inputNewListName.val().length <= 50) {
        eventbus.send('database_insert_new_list', inputNewListName.val(), function (error, reply) {
            if (reply['body']['updated'] != null) {
                console.log('new list created');
                newListModal.modal('close');
                getLists();
            } else {
                //tuli error
            }
        });
    }
}

function getLists() {
    eventbus.send('database_get_lists', {}, function (error, reply) {
        fillLists(reply.body['results']);
    });
}

function fillLists(lists) {
    listsTable.empty();
    if (lists.length > 0) {
        $.each(lists, function (i) {
            listsTable.append($.parseHTML(
                '<tr class="cursor" onclick="openList('+ lists[i][0] + ',\'' + lists[i][1] +'\')">' +
                    '<td>' +
                        '<span class=" grey-text text-darken-1">' + (i + 1) + '</span>' +
                    '</td>' +
                    '<td>' +
                        '<span class="content-key grey-text text-darken-1">' + safe_tags_replace(lists[i][1]) + '</span>' +
                    '</td>' +
                '</tr>'
            ));
        });
    } else {
        listsTable.append($.parseHTML(
            '<h5>No lists</h5>'
        ));
    }
}

function openList(listId, listName) {
    eventbus.send('database_get_list_entries', listId, function (error, reply) {
        console.log('opened list', listId);
        fillMovies(reply.body['rows'], listName, listId);
    });
}

function fillMovies(resultRows, listName, listId) {
    listContent.empty();
    addListTitle(listName);
    addListBody(resultRows, listId);
}

function addListTitle(title) {
    listContent.append($.parseHTML(
        '<div class="row">' +
            '<div class="card z-depth-0">' +
                '<div class="card-title">' +
                    '<span class="light grey-text text-lighten-1 not-found">' +
                        title +
                    '</span>' +
                '</div>' +
            '</div>' +
        '</div>'
    ));
}

function addListBody(data, listId) {
    console.log(data);
    var timeout = 0;
    $.each(data, function (i) {
        setTimeout(function () {
            var posterPath = "";
            var movie = data[i];
            if (movie['Image'] !== "") {
                posterPath = 'https://image.tmdb.org/t/p/w342' + movie['Image'];
            } else {
                posterPath = '/static/img/nanPosterBig.jpg'
            }

            var movieId = movie['MovieId'];
            var cardId = 'card_' + movieId;

            listContent.append(
                $.parseHTML(
                    '<div class="col s12 m12 l6 xl4" id="' + cardId + '">' +
                    '<div class="card horizontal z-depth-0">' +
                    '<div class="card-image">' +
                    '<img class="series-poster search-object-series" src="' + posterPath + '" alt="Poster for movie: ' +
                    movie['Title'] + '" onclick="openMovie(' + movieId + ')">' +
                    '</div>' +
                    '<div class="card-stacked truncate">' +
                    '<div class="card-content">' +
                    '<a class="truncate content-key search-object-series black-text home-link" onclick="openMovie(' + movieId + ')">' +
                    movie['Title'] +
                    '</a>' +
                    '<span>' + movie['Year'] + '</span>' +
                    '</div>' +
                    '<div class="card-action">' +
                    '<a class="search-object-series red-text home-link" onclick="removeFromList(' + movieId + ',' + listId + ')">' + lang['HISTORY_REMOVE'] + '</a>' +
                    '</div>' +
                    '</div>' +
                    '</div>' +
                    '</div>'
                )
            );
        }, timeout += 25);
    });
}

function openMovie(movieId) {
    location.href = 'movies/?id=' + movieId;
}

function removeFromList(movieId, listId) {
    eventbus.send('database_remove_from_list',
        {
            'listId': listId.toString(),
            'movieId': movieId.toString()
        }, function (error, reply) {
            if (reply['body']['updated'] != null) {
                var id = 'card_' + movieId;
                console.log('REMOVED', id);
                $(document.getElementById(id)).remove();
            }
        });
}