package com.soywiz.korui.light

import com.soywiz.korio.async.EventLoop
import com.soywiz.korio.async.EventLoopFactory
import com.soywiz.korio.lang.Closeable
import javax.swing.SwingUtilities
import javax.swing.Timer

class EventLoopFactoryAwt : EventLoopFactory() {
	override fun createEventLoop(): EventLoop = EventLoopAwt()
}

class EventLoopAwt : EventLoop() {
	override fun setImmediateInternal(handler: () -> Unit) {
		if (SwingUtilities.isEventDispatchThread()) {
			handler()
		} else {
			SwingUtilities.invokeLater { handler() }
		}
	}

	override fun setTimeoutInternal(ms: Int, callback: () -> Unit): Closeable {
		val timer = Timer(ms, {
			if (SwingUtilities.isEventDispatchThread()) {
				callback()
			} else {
				SwingUtilities.invokeLater { callback() }
			}
		})
		timer.isRepeats = false
		timer.start()
		return Closeable { timer.stop() }
	}

	override fun setIntervalInternal(ms: Int, callback: () -> Unit): Closeable {
		val timer = Timer(ms, {
			if (SwingUtilities.isEventDispatchThread()) {
				callback()
			} else {
				SwingUtilities.invokeLater { callback() }
			}
		})
		timer.isRepeats = true
		timer.start()
		return Closeable { timer.stop() }
	}
}