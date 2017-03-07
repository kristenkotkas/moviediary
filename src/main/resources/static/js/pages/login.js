fallback.ready(['jQuery'], function () {
    $('#lang-est').hover(
        function () {
            $('#login-body').attr('background', '/static/img/Estonia_flag.jpg');
        }, function () {
            $('#login-body').attr('background', '');
        }
    );

    $('#lang-eng').hover(
        function () {
            $('#login-body').attr('background', '/static/img/Union_Jack.jpg');
        }, function () {
            $('#login-body').attr('background', '');
        }
    );

    $('#lang-ger').hover(
        function () {
            $('#login-body').attr('background', '/static/img/German_flag.jpg');
        }, function () {
            $('#login-body').attr('background', '');
        }
    );
});