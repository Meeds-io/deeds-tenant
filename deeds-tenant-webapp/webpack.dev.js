const path = require('path');
const merge = require('webpack-merge');
const webpackCommonConfig = require('./webpack.common.js');

// the display name of the war
const app = 'deeds-tenant';

const serverPath = "/exo-server";

let config = merge(webpackCommonConfig, {
  output: {
    path: path.resolve(`${serverPath}/webapps/${app}/`),
    filename: 'js/[name].js',
    libraryTarget: 'amd'
  },
  devtool: 'inline-source-map'
});

module.exports = config;
