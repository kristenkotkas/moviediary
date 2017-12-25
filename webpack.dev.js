const webpack = require('webpack');
const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');

module.exports = new Merge(CommonConfig, {
  output: {
    filename: '[name].js'
  },
  devtool: 'cheap-eval-source-map',
  devServer: {
    hot: true,
    hotOnly: true,
    historyApiFallback: true,
    overlay: true,
    contentBase: 'src',
    publicPath: '/'
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: JSON.stringify('development')
      }
    }),
    new webpack.NamedModulesPlugin()
  ]
});
