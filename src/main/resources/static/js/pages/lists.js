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
var listTitleHolder = $('#list-title-holder');
var listContainer = $('#list-container');
var btnDeleteList = $('#delete-list');
var modalDeleteList = $('#modal-delete-list');
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
    unboundOnClick();
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
        unboundOnClick();
        fillMovies(reply.body['rows'], listName, listId);
    });
}

function fillMovies(resultRows, listName, listId) {
    addListTitle(listName, listId);
    addListBody(resultRows, listId);
}

function addListTitle(title, listId) {
    listTitleHolder.empty().append($.parseHTML(
        '<div class="row">' +
            '<div class="card z-depth-0">' +
                '<div class="card-content">' +
                    '<div class="card-title">' +
                        '<span class="light grey-text text-darken-2 list-title" id="list-title">' +
                            title +
                        '</span>' +
                    '</div>' +
                    '<a class="home-link cursor blue-text text-darken-2" ' +
                        'onclick="changeNameOnClick(' + listId + ',' + '\'' + title + '\'' + ')">Muuda nimekirja nime</a><br>' +
                    '<a class="home-link cursor red-text" onclick="openDeleteModal(' + listId + ')">Kustuta nimekiri</a>' +
                '</div>' +
            '</div>' +
        '</div>'
    ));
}

function changeNameOnClick(listId, title) {
    var element = $('#list-title');
    element.empty().append(
        $.parseHTML(
            '<div class="input-field custom-input">' +
                '<input class="custom-input-field grey-text" id="changeNameInput" type="text" value="' + title + '" data-length="50">' +
            '</div>' +
            '<a class="btn z-depth-0 red lighten-2" onclick="addListTitle(\'' + title + '\',' + listId +')">Cancel</a>' +
            '<a> </a>' +
            '<a class="btn z-depth-0 green lighten-2" id="save-name-' + listId + '">Save</a>'
        )
    );
    $(document.getElementById('save-name-' + listId)).click(function () {
        changeListName($('#changeNameInput').val(), listId);
    });
}

function changeListName(title, listId) {
    if (title.length > 0 && title.length <= 50) {
        eventbus.send('database_change_list_name',
            {
                'listName': title.toString(),
                'listId': listId.toString()
            }, function (error, reply) {
                if (reply['body']['updated'] != null) {
                    addListTitle(title, listId);
                    getLists();
                }
            }
        )
    }
}

function openDeleteModal(listId) {
    console.log('delete list modal open');
    modalDeleteList.modal('open');
    btnDeleteList.click(function () {
        deleteList(listId);
    });
}

function deleteList(listId) {

}

function unboundOnClick() {
    btnDeleteList.off('click').off('keyup');
}

function addListBody(data, listId) {
    console.log(data);
    var timeout = 0;
    listContainer.empty();
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

            listContainer.append(
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
        }, timeout += 0);
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