package com.soywiz.korui.ui

import com.soywiz.korim.bitmap.Bitmap
import com.soywiz.korio.async.asyncFun
import com.soywiz.korio.async.spawnAndForget
import com.soywiz.korio.vfs.VfsFile
import com.soywiz.korui.geom.IRectangle
import com.soywiz.korui.geom.len.Length
import com.soywiz.korui.geom.len.percent
import com.soywiz.korui.geom.len.pt
import com.soywiz.korui.geom.len.px
import com.soywiz.korui.light.LightClickEvent
import com.soywiz.korui.light.LightComponents
import com.soywiz.korui.style.Style

open class Component(val lc: LightComponents, val handle: Any) {
	var style = Style()

	var valid = false
	val actualBounds: IRectangle = IRectangle()

	var parent: Container? = null
		set(newParent) {
			if (newParent != null) {
				newParent.children -= this
			}
			field = newParent
			newParent?.children?.add(this)
			lc.setParent(handle, newParent?.handle)
			newParent?.invalidate()
		}

	val children = arrayListOf<Component>()

	fun relayout() {
		if (valid) return
		//println("$this: relayout")
		valid = true
		relayoutInternal()
		for (child in children) child.relayout()
		setBoundsInternal(actualBounds)
	}

	open protected fun relayoutInternal() {
		//println("relayoutInternal: $this")
	}

	fun invalidate() {
		valid = false
		parent?.invalidate()
		invalidateDescendants()
	}

	fun invalidateDescendants() {
		valid = false
		for (child in children) child.invalidateDescendants()
	}

	open fun setBoundsInternal(bounds: IRectangle) {
		//println("$this($parent): $bounds")
		lc.setBounds(handle, bounds.x, bounds.y, bounds.width, bounds.height)
	}

	fun onClick(handler: suspend () -> Unit) {
		lc.setEventHandler<LightClickEvent>(handle) {
			spawnAndForget(handler)
		}
	}

	var visible: Boolean = false
		set(value) {
			field = value
			lc.setVisible(handle, value)
		}

	override fun toString(): String = javaClass.simpleName
}

open class Container(lc: LightComponents, type: String) : Component(lc, lc.create(type)) {
	fun <T : Component> add(other: T): T {
		other.parent = this
		return other
	}
}

class Frame(lc: LightComponents, title: String) : Container(lc, LightComponents.TYPE_FRAME) {
	var title: String = ""
		get() = field
		set(value) {
			lc.setText(handle, value)
		}

	init {
		this.title = title
	}

	open suspend fun dialogOpenFile(filter: String = ""): VfsFile = asyncFun {
		if (!lc.insideEventHandler) throw IllegalStateException("Can't open file dialog outside an event")
		lc.dialogOpenFile(handle, filter)
	}

	open suspend fun prompt(message: String): String = asyncFun {
		lc.dialogPrompt(handle, message)
	}

	open suspend fun alert(message: String): Unit = asyncFun {
		lc.dialogAlert(handle, message)
	}

	// @TODO: @FIXME: JTransc 0.5.3 treeshaking doesn't include this!
	override fun relayoutInternal() {
		for (child in children) {
			child.actualBounds.set(0, 0, child.style.computedWidth.calc(actualBounds.width), child.style.computedHeight.calc(actualBounds.height))
		}
	}

	override fun setBoundsInternal(bounds: IRectangle) {
	}
}

class Button(lc: LightComponents, text: String) : Component(lc, lc.create(LightComponents.TYPE_BUTTON)) {
	var text: String = ""
		get() = field
		set(value) {
			field = value
			lc.setText(handle, value)
		}

	init {
		style.size.setTo(100.percent, 32.pt)
		this.text = text
	}
}

class Progress(lc: LightComponents, current: Int, max: Int) : Component(lc, lc.create(LightComponents.TYPE_PROGRESS)) {
	var current: Int = 0
		get() = field
		set(value) {
			field = value
			lc.setAttributeInt(handle, "current", value)
		}

	var max: Int = 0
		get() = field
		set(value) {
			field = value
			lc.setAttributeInt(handle, "max", value)
		}

	fun set(current: Int, max: Int) {
		this.current = current
		this.max = max
	}

	init {
		style.size.setTo(100.percent, 32.pt)
		set(current, max)
	}
}

class Spacer(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_CONTAINER)) {
	init {
		style.size.setTo(100.percent, 32.pt)
	}
}

class Image(lc: LightComponents) : Component(lc, lc.create(LightComponents.TYPE_IMAGE)) {
	var image: Bitmap? = null
		set(newImage) {
			if (newImage != null) {
				style.size.setTo(newImage.width.px, newImage.height.px)
			}
			lc.setImage(handle, newImage)
			invalidate()
		}
}

//fun Application.createFrame(): Frame = Frame(this.light)

fun <T : Component> T.setSize(width: Length, height: Length) = this.apply { this.style.size.setTo(width, height) }

fun Container.button(text: String) = add(Button(this.lc, text))
inline fun Container.button(text: String, clickHandler: suspend () -> Unit) = add(Button(this.lc, text).apply { onClick(clickHandler) })
inline fun Container.progress(current: Int, max: Int) = add(Progress(this.lc, current, max))
inline fun Container.image(bitmap: Bitmap, callback: Image.() -> Unit) = add(Image(this.lc).apply { image = bitmap; callback() })
inline fun Container.image(bitmap: Bitmap) = add(Image(this.lc).apply { image = bitmap })
inline fun Container.spacer() = add(Spacer(this.lc))

inline fun Container.vertical(callback: VerticalLayout.() -> Unit): VerticalLayout = add(VerticalLayout(this.lc).apply { callback() })
inline fun Container.horizontal(callback: HorizontalLayout.() -> Unit): HorizontalLayout = add(HorizontalLayout(this.lc).apply { callback() })
