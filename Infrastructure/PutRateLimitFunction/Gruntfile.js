module.exports = function(grunt) {
	grunt.loadNpmTasks('grunt-aws-lambda');

	grunt.initConfig({
		lambda_invoke: {
			default: {
				options: {
					file_name: 'put-rate-limit-function.js',
					event: 'test/event.json'
				}
			}
		},
		lambda_package: {
			default: {
				// options: {
				// 	dist: 'dist'
				// }
			}
		}
	});
};