const path = require('path');

let config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    metamaskLoginExtension: './src/main/webapp/vue-app/login/main.js',
    metamaskRegisterExtension: './src/main/webapp/vue-app/register-extension/main.js',
    metamaskRegisterForm: './src/main/webapp/vue-app/register-form/main.js',
  },
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
          'eslint-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
          'eslint-loader',
        ]
      }
    ]
  }
};

module.exports = config;
