fallback.ready(['jQuery'], function () {
    $.ajax({
        url: '/private/api/v1/views/count',
        type: 'GET',
        contentType: 'application/json',
        success: function (data) {
            $('#UserCount').text("User Count: " + data);
        },
        error: function (e) {
            console.log(e.message())
        }
    });
});