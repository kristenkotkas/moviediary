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

eventbus.onopen = function () {
    var lang;
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
                '<tr class="cursor" onclick="openList('+ lists[i][0] +')">' +
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

function openList(listId) {
    console.log('opened list', listId);
}