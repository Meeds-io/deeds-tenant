const path = require('path');
const { merge } = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// the display name of the war
const app = 'deeds-tenant';

const serverPath = "/deeds-server";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${serverPath}/webapps/${app}/`),
    filename: 'js/[name].js',
    libraryTarget: 'amd'
  },
  mode: 'development',
  devtool: 'inline-source-map'
});

module.exports = config;
