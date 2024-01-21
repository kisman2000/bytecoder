import imgui.ImGui
import imgui.app.Application
import imgui.app.Configuration
import imgui.flag.ImGuiCol
import imgui.flag.ImGuiInputTextFlags
import imgui.flag.ImGuiWindowFlags
import org.lwjgl.glfw.GLFW
import org.lwjgl.glfw.GLFWErrorCallback

class Gui : Application() {
    override fun configure(
        config : Configuration
    ) {
        colorBg.set(0f, 0f, 0f, 0f)

        config.title = "bytecoder"
        config.width = 515
        config.height = 690

        GLFWErrorCallback.createPrint(System.err).set()

        if(!GLFW.glfwInit()) {
            throw IllegalStateException("Unable to initialize GLFW")
        }

        GLFW.glfwWindowHint(GLFW.GLFW_TRANSPARENT_FRAMEBUFFER, 1)
        GLFW.glfwWindowHint(GLFW.GLFW_RESIZABLE, 0)
    }

    override fun process() {
        ImGui.styleColorsDark()

        val style = ImGui.getStyle()

        style.frameBorderSize = 1f

        val window = style.getColor(ImGuiCol.WindowBg)

        style.setColor(ImGuiCol.WindowBg, window.x, window.y, window.z, 0.6f)

        val frame = style.getColor(ImGuiCol.FrameBg)

        style.setColor(ImGuiCol.FrameBg, frame.x, frame.y, frame.z, frame.w / 2f)

        ImGui.pushStyleColor(ImGuiCol.ChildBg, frame.x, frame.y, frame.z, frame.w / 2f)

        ImGui.begin("bytecoder", ImGuiWindowFlags.NoResize or ImGuiWindowFlags.NoDecoration)

        val w = intArrayOf(1)
        val h = intArrayOf(1)

        GLFW.glfwGetWindowSize(handle, w, h)

        ImGui.setWindowPos(0f, 0f)
        ImGui.setWindowSize(w[0].toFloat(), h[0].toFloat())

        ImGui.text("Java code")
        ImGui.inputTextMultiline("## Java code", JAVA_CODE, 500f, 300f, ImGuiInputTextFlags.AllowTabInput)

        ImGui.text("Byte code")
        ImGui.inputTextMultiline("## Byte code", BYTE_CODE, 500f, 300f, ImGuiInputTextFlags.AllowTabInput)

        if(ImGui.button("Convert to byte code")) {
//            generatePhantoms()
            funny()
        }

        ImGui.button("Convert to java objectweb2 code")

//        ImGui.inputTextMultiline("Java code", JAVA_CODE, 500f, 300f)

        /*ImGui.beginTabBar("Texts")

        ImGui.beginTabItem("Java code")
        ImGui.endTabItem()
        ImGui.endTabBar()

        ImGui.beginTabBar("Texts1")

        ImGui.beginTabItem("Byte code"*//*, ImBoolean(false)*//*)
        ImGui.inputTextMultiline("Byte code", BYTE_CODE, 500f, 300f)
        ImGui.endTabItem()

        ImGui.endTabBar()*/


        ImGui.end()
        ImGui.popStyleColor()
    }
}