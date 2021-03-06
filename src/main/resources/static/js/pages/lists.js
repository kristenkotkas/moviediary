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
var deletedListsContainer = $('#deleted-lists-table');
var listCollapsible = $('#lists-coll');
var deletedCollapsible = $('#deleted-coll');
var lang;
var listData;
var sorterTitles;
var sorterType;
var isMobile = false;

inputNewListName.keyup(function (e) {
  if (e.keyCode === 13) {
    createNewList();
  }
});

eventbus.onopen = function () {
  eventbus.send("translations", getCookie("lang"), function (error, reply) {
    lang = reply.body;
    enableParameterListsLoading(eventbus, lang);
    fillSorterJSONs(lang);
    getLists();
    getDeletedLists();

    if ($(window).width() > 600) {
      openCollapsible();
    } else {
      listCollapsible.collapsible('open', 0);
      isMobile = true;
    }
  });
};

function openCollapsible() {
  listCollapsible.collapsible('open', 0);
}

function closeCollabsible() {
  listCollapsible.collapsible('close', 0);
  deletedCollapsible.collapsible('close', 0);
}

function fillSorterJSONs(lang) {
  sorterTitles = {
    titleSort123123: lang['LISTS_FROM_A_TO_Z'],
    titleSort123321: lang['LISTS_FROM_Z_TO_A'],
    yearSort123123: lang['LISTS_OLDEST_FIRST'],
    yearSort123321: lang['LISTS_NEWEST_FIRST'],
    addedSort123123: lang['LISTS_ADDED_EARLIER'],
    addedSort123321: lang['LISTS_ADDED_LATER']
  };
}

function modalClose() {
  //console.log('modal closed');
  inputNewListName.val('');
  unboundOnClick();
}

function createNewList() {
  if (inputNewListName.val().length > 0 && inputNewListName.val().length <= 50) {
    eventbus.send('database_insert_new_list', inputNewListName.val(), function (error, reply) {
      if (reply['body']['updated'] != null) {
        //console.log('new list created');
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
    fillLists(reply.body['rows']);
  });
}

function fillLists(lists) {
  listsTable.empty();
  if (lists.length > 0) {
    $.each(lists, function (i) {
      listsTable.append($.parseHTML(
          '<tr class="cursor" onclick="openList(' + lists[i]['Id'] + ')">' +
          '<td>' +
          '<span class="content-key grey-text text-darken-1">' + safe_tags_replace(lists[i]['ListName']) + '</span><br>' +
          '<span class="grey-text text-darken-1" id="list-size-' + lists[i]['Id'] + '">0 ' + lang['HOME_MOVIES'] + '</span>' +
          '</td>' +
          '</tr>'
      ));
    });
    getListsSize();
  } else {
    listsTable.append($.parseHTML(
        '<h5>' + lang['MOVIES_NO_LISTS'] + '</h5>'
    ));
  }
}

function getListsSize() {
  eventbus.send('database_get_lists_size', {}, function (error, reply) {
    fillListsSize(reply.body['rows']);
  });
}

function fillListsSize(rows) {
  //console.log(rows);
  $.each(rows, function (i) {
    var percent = Math.round(rows[i]['seen'] / rows[i]['count'] * 100);
    $(document.getElementById('list-size-' + rows[i]['id'])).empty().append(
        $.parseHTML(
            //'<div style="background: lightblue; height: 1em; width: ' + percent + '%"></div>' +
            '<span>' + rows[i]['count'] + ' ' +
            getSingPlur(lang['HOME_MOVIE'], lang['HOME_MOVIES'], rows[i]['count'])
            + ' | ' + rows[i]['seen'] + ' ' + lang['LISTS_SEEN'] +
            '</span><br>'
        )
    );
  });
}

function openList(listId) {
  eventbus.send('database_get_list_entries', listId.toString(), function (error, reply) {
    //console.log(reply);
    unboundOnClick();
    if (isMobile) {
      closeCollabsible();
    }
    listData = reply.body['rows'];
    if (listData.length > 0) {
      fillMovies(listData, listData[0]['ListName'], listId);
    } else {
      eventbus.send('database_get_list_name', listId.toString(), function (error, reply) {
        fillMovies(listData, reply.body['rows'][0]['ListName'], listId);
      });
    }
    replaceUrlParameter("id", listId);
  });
}

/*
 - date added
 - year
 - title
 */
function fillSortedList(sorter, type) {
  $(document.getElementById('sort-dropdown')).empty()
      .append(lang['LISTS_SORT'] + ' | ' + sorterTitles[sorter.name + type]);
  addListBody(getSorted(listData, sorter, type), listData[0]['ListId']);
}

function getSorted(data, sorter, type) {
  // reply.body['rows'];
  if (type === 123) {
    return data.sort(sorter);
  } else if (type === 321) {
    return data.sort(sorter).reverse();
  }
}

function yearSort123(a, b) {
  return ((a['Year'] == b['Year']) ? 0 :
      ((a['Year'] > b['Year']) ? 1 : -1));
}

function titleSort123(a, b) {
  return ((a['Title'] == b['Title']) ? 0 :
      ((a['Title'] > b['Title']) ? 1 : -1));
}

function addedSort123(a, b) {
  return ((a['Time'] == b['Time']) ? 0 :
      ((a['Time'] > b['Time']) ? 1 : -1));
}

function fillMovies(resultRows, listName, listId) {
  addListTitle(listName, listId);
  if (resultRows.length > 0) {
    addListBody(resultRows, listId);
  } else {
    addEmptyListBody();
  }
}

function addEmptyListBody() {
  listContainer.empty();
  listContainer.append(
      $.parseHTML(
          '<div class="card z-depth-0">' +
          '<div class="card-content">' +
          '<div class="card-title">' +
          '<span class="light grey-text text-darken-2 list-title">' + lang['LISTS_NO_MOVIES'] + '</span>' +
          '</div>' +
          '</div>' +
          '</div>'
      )
  );
}

function addListTitle(title, listId) {
  listTitleHolder.empty().append($.parseHTML(
      '<div class="last-row">' +
      '<div class="card z-depth-0">' +
      '<div class="card-content">' +
      '<div class="card-title">' +
      '<span class="light grey-text text-darken-2 list-title" id="list-title">' +
      '<div onclick="changeNameOnClick(' + listId + ',' + '\'' + title + '\'' + ')">' + title + '</div>' +
      '</span>' +
      '</div>' +
      '<a class="home-link cursor blue-text text-darken-2" ' +
      'onclick="changeNameOnClick(' + listId + ',' + '\'' + title + '\'' + ')">'
      + lang['LISTS_CHANGE_TITLE'] + '</a><br>' +
      '<a class="home-link cursor red-text" onclick="openDeleteModal(' + listId + ')">'
      + lang['LISTS_DELETE_LIST'] + '</a><br>' +
      '<a class="dropdown-button home-link cursor" data-activates="sortDropdown" href="#" id="sort-dropdown">' +
      lang['LISTS_SORT'] + '</a>' +
      '<ul id="sortDropdown" class="dropdown-content">' +
      '<li onclick="fillSortedList(titleSort123, 123, ' + listId + ')"><a class="grey-text text-darken-2">' +
      lang['LISTS_FROM_A_TO_Z'] + '<i class="fa fa-sort-alpha-asc" aria-hidden="true"></i></a></li>' +
      '<li onclick="fillSortedList(titleSort123, 321, ' + listId + ')"><a class="grey-text text-darken-2">' +
      lang['LISTS_FROM_Z_TO_A'] + '<i class="fa fa-sort-alpha-desc" aria-hidden="true"></i></a></li>' +
      '<li class="divider"></li>' +
      '<li onclick="fillSortedList(yearSort123, 123, ' + listId + ')"><a class="grey-text text-darken-2">' +
      lang['LISTS_OLDEST_FIRST'] + '<i class="fa fa-sort-numeric-asc" aria-hidden="true"></i></a></li>' +
      '<li onclick="fillSortedList(yearSort123, 321, ' + listId + ')"><a class="grey-text text-darken-2">' +
      lang['LISTS_NEWEST_FIRST'] + '<i class="fa fa-sort-numeric-desc" aria-hidden="true"></i></a></li>' +
      '<li class="divider"></li>' +
      '<li onclick="fillSortedList(addedSort123, 123, ' + listId + ')"><a class="grey-text text-darken-2">' +
      lang['LISTS_ADDED_EARLIER'] + '<i class="fa fa-sort-amount-asc" aria-hidden="true"></i></a></li>' +
      '<li onclick="fillSortedList(addedSort123, 321, ' + listId + ')"><a class="grey-text text-darken-2">' +
      lang['LISTS_ADDED_LATER'] + '<i class="fa fa-sort-amount-desc" aria-hidden="true"></i></a></li>' +
      '</ul>' +
      '</div>' +
      '</div>' +
      '</div>'
  ));
  $('.dropdown-button').dropdown({
        inDuration: 300,
        outDuration: 225,
        constrainWidth: false, // Does not change width of dropdown to that of the activator
        hover: false, // Activate on hover
        gutter: 0, // Spacing from edge
        belowOrigin: false, // Displays dropdown below the button
        alignment: 'left', // Displays dropdown with edge aligned to the left of button
        stopPropagation: false // Stops event propagation
      }
  );
}

function changeNameOnClick(listId, title) {
  var element = $('#list-title');
  element.empty().append(
      $.parseHTML(
          '<div class="input-field custom-input">' +
          '<input class="custom-input-field grey-text" id="changeNameInput" type="text" value="' + title + '" ' +
          'data-length="50">' +
          '</div>' +
          '<a class="btn z-depth-0 red lighten-2" onclick="addListTitle(\'' + title + '\',' + listId + ')">'
          + lang['MOVIES_CANCEL'] + '</a>' +
          '<a> </a>' +
          '<a class="btn z-depth-0 green lighten-2" id="save-name-' + listId + '">' + lang['LISTS_SAVE'] + '</a>'
      )
  );
  $(document.getElementById('save-name-' + listId)).click(function () {
    changeListName($('#changeNameInput').val(), listId);
  });

  $('#changeNameInput').keyup(function (e) {
    if (e.keyCode === 13) {
      changeListName($('#changeNameInput').val(), listId);
    }
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
  //console.log('delete list modal open');
  modalDeleteList.modal('open');
  btnDeleteList.click(function () {
    deleteList(listId);
  });
}

function deleteList(listId) {
  eventbus.send('database_delete_list', listId.toString(), function (error, reply) {
    if (reply['body']['updated'] != null) {
      //console.log('deleted list', listId);
      modalDeleteList.modal('close');
      listTitleHolder.empty();
      listContainer.empty();
      getLists();
      getDeletedLists();
    }
  });
}

function getDeletedLists() {
  eventbus.send('database_get_deleted_lists', {}, function (error, reply) {
    fillDeletedLists(reply.body['results']);
  });
}

function fillDeletedLists(lists) {
  deletedListsContainer.empty();
  if (lists.length > 0) {
    $.each(lists, function (i) {
      deletedListsContainer.append($.parseHTML(
          '<tr>' +
          '<td class="left">' +
          '<span class="content-key grey-text text-darken-1">' + safe_tags_replace(lists[i][1]) + '</span>' +
          '</td>' +
          '<td class="right">' +
          '<span class="home-link cursor grey-text text-darken-1" ' +
          'onclick="restoreDeletedList(' + lists[i][0] + ')">' + lang['LISTS_RESTORE'] + '</span>' +
          '</td>' +
          '</tr>'
      ));
    });
  } else {
    deletedListsContainer.append($.parseHTML(
        '<h5>' + lang['MOVIES_NO_LISTS'] + '</h5>'
    ));
  }
}

function restoreDeletedList(listId) {
  eventbus.send('database_restore_deleted_list', listId.toString(), function (error, reply) {
    getLists();
    getDeletedLists();
  });
}

function unboundOnClick() {
  btnDeleteList.off('click').off('keyup');
}

function addListBody(data, listId) {
  listContainer.empty();
  $.each(data, function (i) {

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
            '<div class="card horizontal z-depth-0" id="inner-' + cardId + '">' +
            '<div class="card-image">' +
            '<img class="series-poster search-object-series" src="' + posterPath + '" alt="Poster for movie: ' +
            movie['Title'] + '" onclick="openMovie(' + movieId + ')">' +
            '</div>' +
            '<div class="card-stacked truncate">' +
            '<div class="card-content">' +
            '<a class="truncate content-key search-object-series black-text home-link" onclick="openMovie(' + movieId + ')">' +
            movie['Title'] +
            '</a>' +
            '<span>' + yearNullCheck(movie['Year'], lang) + '</span>' +
            '</div>' +
            '<div class="card-action">' +
            '<span><a class="search-object-series red-text home-link" onclick="removeFromList(' + movieId + ',' + listId + ')">' + lang['HISTORY_REMOVE'] + '</a></span>' +
            '<span id="movie-card-content-' + movieId + '"></span>' +
            '</div>' +
            '</div>' +
            '</div>' +
            '</div>'
        )
    );
    if (movie['Seen'] === 1) {
      decorateSeenMovieCard(movieId);
    }
  });
}

function decorateSeenMovieCard(movieId) {
  $(document.getElementById('inner-card_' + movieId)).addClass('green').addClass('lighten-4');
  $(document.getElementById('movie-card-content-' + movieId)).empty().append(
      $.parseHTML(
          '<i class="fa fa-check right fa-lg white-text" aria-hidden="true"></i>'
      )
  );
}

function openMovie(movieId) {
  location.href = '/private/movies/?id=' + movieId;
}

function removeFromList(movieId, listId) {
  eventbus.send('database_remove_from_list',
      {
        'listId': listId.toString(),
        'movieId': movieId.toString()
      }, function (error, reply) {
        if (reply['body']['updated'] != null) {
          var id = 'card_' + movieId;
          //console.log('REMOVED', id);
          $(document.getElementById(id)).remove();
          getLists();
        }
      });
}

var enableParameterListsLoading = function (eventbus, lang) {
  var loadList = function (eventbus, lang) {
    var query = getUrlParam("id");
    if (query !== null && isNormalInteger(query)) {
      openList(parseInt(query));
    }
  };
  window.onpopstate = function () { //try to load series on back/forward page movement
    loadList(eventbus, lang);
  };
  loadList(eventbus, lang); //load series if url has param
};


function getSingPlur(singularString, pluralString, count) {
  if (count === 1) {
    return singularString
  } else {
    return pluralString;
  }
}
