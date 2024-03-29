const path = require('path');
const ESLintPlugin = require('eslint-webpack-plugin');
const { VueLoaderPlugin } = require('vue-loader')

let config = {
  context: path.resolve(__dirname, '.'),
  // set the entry point of the application
  // can use multiple entry
  entry: {
    womSetup: './src/main/webapp/vue-app/wom-setup-admin/main.js',
    womWalletExtensions: './src/main/webapp/vue-app/wom-wallet-extensions/main.js',
    metamaskLoginExtension: './src/main/webapp/vue-app/login/main.js',
    metamaskRegisterExtension: './src/main/webapp/vue-app/register-extension/main.js',
    metamaskSetupForm: './src/main/webapp/vue-app/register-deed-setup/main.js',
  },
  plugins: [
    new ESLintPlugin({
      files: [
        './src/main/webapp/vue-app/*.js',
        './src/main/webapp/vue-app/*.vue',
        './src/main/webapp/vue-app/**/*.js',
        './src/main/webapp/vue-app/**/*.vue',
      ],
    }),
    new VueLoaderPlugin()
  ],
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules/,
        use: [
          'babel-loader',
        ]
      },
      {
        test: /\.vue$/,
        use: [
          'vue-loader',
        ]
      }
    ]
  }
};

module.exports = config;
