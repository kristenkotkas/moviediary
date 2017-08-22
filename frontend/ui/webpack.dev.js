const webpack = require('webpack');
const Merge = require('webpack-merge');
const CommonConfig = require('./webpack.common');

module.exports = new Merge(CommonConfig, {
  output: {
    filename: '[name].js',
  },
  devtool: 'cheap-eval-source-map',
  devServer: {
    hot: true,
    hotOnly: true,
    historyApiFallback: true,
    overlay: true,
    /*  contentBase: './src/main/assets',
      publicPath: '/'*/
  },
  plugins: [
    new webpack.DefinePlugin({
      'process.env': {
        NODE_ENV: '"development"'
      }
    }),
    new webpack.HotModuleReplacementPlugin({
      // multiStep: true, // better performance with many files
    }),
    new webpack.NamedModulesPlugin(),
  ]
});