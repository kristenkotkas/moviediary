const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');
const webpack = require('webpack');
const CleanWebpackPlugin = require('clean-webpack-plugin');
const FileManagerPlugin = require('filemanager-webpack-plugin');
module.exports = new Merge(CommonConfig, {
  output: {
    filename: '[name].[hash].js',
    publicPath: '/static/dist/'
  },
  devtool: 'source-map',
  plugins: [
    new CleanWebpackPlugin(['./src/main/resources/static/dist']),
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify('development') //todo production
      }
    }),
    /*    new webpack.LoaderOptionsPlugin({
          minimize: true,
          debug: false
        }),*/
    /*    new webpack.optimize.UglifyJsPlugin({
          beautify: false,
          compress: {
            screw_ie8: true
          },
          comments: false,
          sourceMap: false,
        }),*/
    new FileManagerPlugin({
      onEnd: {
        move: [
          {
            source: './src/main/resources/static/dist/index.html',
              destination: './src/main/resources/templates/recommender.hbs'
          }
        ]
      }
    })
  ]
});
