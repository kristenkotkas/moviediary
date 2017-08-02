import React from 'react'
import ReactDOM from 'react-dom'
import {Provider} from 'react-redux'

import store from './store'
import Layout from "./components/layout";

const app = document.getElementById('app');

ReactDOM.render(
    <Provider store={store}>
      <Layout/>
    </Provider>
    , app);