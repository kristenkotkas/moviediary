const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');
const webpack = require('webpack');

module.exports = new Merge(CommonConfig, {
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        'NODE_ENV': JSON.stringify('production')
      }
    }),
    /*new webpack.optimize.UglifyJsPlugin({
     sourceMap: true,
     beautify: true,
     compress: true
     })*/
  ]
});