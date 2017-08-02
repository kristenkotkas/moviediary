const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');
const WebPack = require('webpack');

module.exports = new Merge(CommonConfig, {
  plugins: [
    new WebPack.DefinePlugin({
      'process.env': {
        NODE_ENV: '"production"'
      }
    }),
    new WebPack.optimize.UglifyJsPlugin({
      sourceMap: true,
      beautify: true,
      compress: {
        warnings: false
      }
    }),
    new WebPack.LoaderOptionsPlugin({
      minimize: true
    })
  ]
});