const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');
const webpack = require('webpack');

module.exports = new Merge(CommonConfig, {
  devtool: 'cheap-eval-source-map',
  devServer: {
    historyApiFallback: true
  }
});