let loadingCounter = 0;
const types = {
  today: 1,
  week: 2,
  month: 3,
  year: 4,
  all: 5,
  search: 6
};

function asyncLoadCSS(href) {
  loadingCounter++;
  const ss = document.createElement('link');
  ss.href = href;
  ss.rel = 'stylesheet';
  ss.type = 'text/css';
  ss.media = 'bogus'; //fetch without blocking
  ss.onload = () => {
    ss.media = 'screen'; //render after loaded
    loadingCounter--;
    if (loadingCounter === 0) {
      document.getElementsByTagName('body')[0].className += ' loaded';
    }
  };
  document.getElementsByTagName('head')[0].appendChild(ss);
}

asyncLoadCSS('https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css');
asyncLoadCSS('https://cdnjs.cloudflare.com/ajax/libs/materialize/0.98.2/css/materialize.min.css');
asyncLoadCSS('/css/custom/base.css');

const tagsToReplace = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;'
};

function safeTagsReplace(str) {
  return str.replace(/[&<>]/g, tag => tagsToReplace[tag] || tag);
}

function getCookie(name) {
  const value = '; ' + document.cookie;
  const parts = value.split('; ' + name + '=');
  if (parts.length === 2) {
    return parts.pop().split(';').shift();
  }
}

function getMonth(start, lang) {
  const startArray = start.split(' ');
  const month = startArray[1];
  return startArray[0] + lang[month.toUpperCase()] + ' ' + startArray[2];
}

function getUrlParam(param) {
  location.search.substr(1)
      .split('&')
      .some(item => item.split('=')[0] === param && (param = item.split('=')[1])); // returns first occurrence and stops
  return param;
}

function isNormalInteger(str) {
  const n = Math.floor(Number(str));
  return String(n) === str && n >= 0;
}

//History time interval

function getThisWeek() {
  const result = {};
  const curr = new Date(); // get current date
  const first = curr.getDate() - ((curr.getDay() + 6) % 7); // First day is the day of the month - the day of the week
  const firstday = new Date(curr.setDate(first));
  const lastday = new Date(curr.setDate(curr.getDate() + 6));
  result['start'] = firstday;
  result['end'] = lastday;
  return result;
}

function getThisMonth(index) {
  const result = {};
  const date = new Date();
  const firstday = new Date(date.getFullYear(), date.getMonth() + index, 1);
  const lastday = new Date(date.getFullYear(), date.getMonth() + 1 + index, 0);
  result['start'] = firstday;
  result['end'] = lastday;
  return result;
}

function getThisYear() {
  const result = {};
  const date = new Date();
  const firstday = new Date(date.getFullYear(), 0, 1);
  const lastday = new Date(date.getFullYear(), 11, 31);
  result['start'] = firstday;
  result['end'] = lastday;
  return result;
}

function getYear(year) {
  const result = {};
  const firstday = new Date(year, 0, 1);
  const lastday = new Date(year, 11, 31);
  result['start'] = firstday;
  result['end'] = lastday;
  return result;
}

function minutesToString(minutes, lang) {
  const seconds = minutes * 60;
  const daysString = lang['LOADER_DAY'];
  const hoursString = lang['LODAER_HOUR'];
  const minString = lang['LOADER_MIN'];
  const daysStringPlur = lang['LOADER_DAYS'];
  const hoursStringPlur = lang['LODAER_HOUR'];
  const minStringPlur = lang['LOADER_MIN'];
  let result = '';
  const numdays = Math.floor(seconds / 86400);
  const numhours = Math.floor((seconds % 86400) / 3600);
  const numminutes = Math.floor(((seconds % 86400) % 3600) / 60);
  if (numdays > 0) {
    result += numdays > 1 ? numdays + ' ' + daysStringPlur + ' ' : numdays + ' ' + daysString + ' ';
  }
  if (numhours > 0) {
    result += numhours > 1 ? numhours + ' ' + hoursStringPlur + ' ' : numhours + ' ' + hoursString + ' ';
  }
  if (numminutes > 0) {
    result += numminutes > 1 ? numminutes + ' ' + minStringPlur + ' ' : numminutes + ' ' + minString + ' ';
  }
  return result.length > 0 ? result : '0 ' + minString;
}

function replaceUrlParameter(param, value) {
  let url = location.href;
  const splitted = url.split('?');
  let shouldPushHistory = true;
  if (splitted.length > 1) {
    url = splitted[0];
    if (splitted[1].indexOf(value) !== -1) {
      shouldPushHistory = false;
    }
  }
  url = url + '?' + param + '=' + value;
  if (shouldPushHistory) {
    window.history.pushState('', '', url);
  }
}

export {
  replaceUrlParameter, minutesToString, getYear, getThisYear, getThisMonth,
  getThisWeek, isNormalInteger, getUrlParam, getMonth, getCookie, safeTagsReplace, asyncLoadCSS
};