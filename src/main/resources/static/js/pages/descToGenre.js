////console.log("hello");

const getApiUrl = function () {
  const url = window.location.origin + '/';
  const localUrl = 'http://localhost:8081/';
  return url.indexOf('localhost') !== -1 ? localUrl : url;
};
//https://moviediary.eu/'
//console.log('API', getApiUrl());

const suitableGenres = $("#suitable-genres");
const getGenres = $("#get-genres");
const inputArea = $("#plot-input-area");

getGenres.click(function () {
  loadGenres(inputArea.val());
});

function loadGenres(plot) {
  //console.log("get genres pressed");
  //console.log("PLOT", plot);

  $.ajax({
    type: "POST",
    url: getApiUrl() + "public/api/v1/genres",
    data: JSON.stringify({ "description": plot}),
    success: function (res) {
      fillGenres(res);
    }
  });
}

function fillGenres(result) {
  //console.log("RESULT", result);
  result = $.parseJSON(result);
  suitableGenres.empty().append(
      $.parseHTML(
          '<ul>' +
          '<li><span class="descToGenreItem"><i class="fa fa-chevron-right" aria-hidden="true"></i> ' + result['best'] +'</span></li>' +
          '<li><span class="descToGenreItem"><i class="fa fa-chevron-right" aria-hidden="true"></i> ' + result['second'] +'</span></li>' +
          '<li><span class="descToGenreItem"><i class="fa fa-chevron-right" aria-hidden="true"></i> ' + result['third'] +'</span></li>' +
          '</ul>'
      )
  );
}
