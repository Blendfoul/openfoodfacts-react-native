module.exports = {
  dependency: {
    platforms: {
      android: {
        sourceDir: './android',
        packageImportPath: 'import com.openfoodfacts.OpenFoodFactsPackage;',
        packageInstance: 'new OpenFoodFactsPackage()',
      },
    },
  },
};
