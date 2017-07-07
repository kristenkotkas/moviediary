const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');
const webpack = require('webpack');

module.exports = new Merge(CommonConfig, {
  devtool: 'cheap-eval-source-map',
  output: {
    sourceMapFilename: '[name].map'
  },
  plugins: [
    /*new webpack.optimize.UglifyJsPlugin({
     sourceMap: true,
     beautify: true
     }),*/
  ],
  devServer: {
    port: 8080,
    publicPath: 'http://localhost:8080/',
    proxy: {
      '**': {
        target: 'http://localhost:8081',
        secure: false,
        prependPath: false
      }
    }
  }
});