import React from 'react'
import ReactDOM from 'react-dom'
import {Provider} from 'react-redux'
import {Route, Router} from 'react-router'
import {createBrowserHistory} from 'history'

import store from './store'
import Layout from "./login/components/layout";

const app = document.getElementById('app');

const browserHistory = createBrowserHistory();

ReactDOM.render(
    <Provider store={store}>
      <Router history={browserHistory}>
        <Route component={Layout}/>
      </Router>
    </Provider>
    , app);