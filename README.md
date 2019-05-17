# Thanos
Removing class files in jar



## usage

**Step 1.** Add the JitPack repository to your build file

Add it in your root build.gradle at the end of repositories:

```
	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```

**Step 2.** Add the dependency

```
	dependencies {
	        classpath 'com.github.zjutkz:Thanos:0.0.1'
	}
```

**Step 3.** Config extensions

```
apply plugin: 'thanos'
excludes {
    jarExcludes = ['android.support.constraint.ConstraintLayout']
}
```

