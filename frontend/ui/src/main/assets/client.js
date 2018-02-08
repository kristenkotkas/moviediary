// @flow
import React from 'react';
import ReactDOM from 'react-dom';
import {Provider} from 'react-redux';
import {Route, Router} from 'react-router';
import {ConnectedRouter} from 'react-router-redux';
import createHistory from 'history/createBrowserHistory';

import store from './store';
import Layout from './components/layout';
import Movies from "./components/movies/Movies";
import '../resources/css/Main.css';

const app = document.getElementById('app');
const history = createHistory();

ReactDOM.render(
  <Provider store={store}>
    <ConnectedRouter history={history}>
      <div>
        <Route exact path={'/'} component={Layout}/>
        <Route exact path={'/movies'} component={Movies}/>
      </div>
    </ConnectedRouter>
  </Provider>, app);
