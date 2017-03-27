$(".sidebar-collapse").sideNav(); //sidebar initialization
$(".datepicker").pickadate({ //calendar initialization
    //http://amsul.ca/pickadate.js/date/#options
    selectMonths: true,
    selectYears: 10,
    firstDay: 1
});
$('.tooltipped').tooltip({ //tooltips initialization
    delay: 150,
    position: 'top',
    html: true
});
$('.modal').modal(); //movies modal initialization
$('body').addClass('loaded'); //remove loader