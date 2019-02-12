# Introduction
Here is a Project called javasist-util, but in fact this is a project based on ASM (LOL

The main purpose of this project is help hookling method in your java/kotlin project. 
For example:

```java
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        var a = String.javaClass
        setContentView(R.layout.activity_main)
        findViewById<TextView>(R.id.tv_1).setOnClickListener {
            Toast.makeText(this, "4321", Toast.LENGTH_SHORT).show()
        }
        
        var c = Application()
    }

}
```

For the code above if I want to change the text "4321" to 1234 is easy, but if I want to do more when every Toast.makeText method is called including library project and dependencies modules, then it seems not easy to do.

If you use this projcet, here is a simple way:

```java 
    @HookParams(targetClass = Toast.class)
    public static Toast makeText(Context context, CharSequence text, int duration) {
        //do your own business
        return Toast.makeText(context, text, duration);
    }
```

Just write the the code above any where in your project, then it will done.
For each Toast.makeText method is called exclude the one above, all the method will be hooked by the above static method. Then you know what to do next.

Enjoy

