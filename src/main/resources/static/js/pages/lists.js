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
var listsCollapsable = $('#lists-coll');

eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        listsCollapsable.collapsible('open', 0);
    });
};

function modalClose() {
    console.log('modal closed');
    inputNewListName.val('');
}

function createNewList() {
    if (inputNewListName.val().length > 0) {
        console.log('new list created');
        newListModal.modal('close');
    }
}

function fillLists(data) {

}