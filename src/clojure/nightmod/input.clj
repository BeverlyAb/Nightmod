(ns nightmod.input
  (:require [play-clj.core :as play-clj])
  (:import [java.awt.event ComponentAdapter KeyListener]
           [com.badlogic.gdx.backends.lwjgl LwjglInput]
           [nightmod KeyCodeConverter]
           [org.lwjgl.input Keyboard]
           [org.lwjgl.opengl InputImplementation]))

(defn awt->lwjgl
  "Translates key code from AWT to LWJGL."
  [key]
  (KeyCodeConverter/translateFromAWT key))

(defn awt->libgdx
  "Translates key code from AWT to LibGDX."
  [key]
  (LwjglInput/getGdxKeyCode (awt->lwjgl key)))

(defn pass-key-events!
  "Passes key events to the game."
  [window game]
  (let [key-buf-field (doto (.getDeclaredField Keyboard "keyDownBuffer")
                        (.setAccessible true))
        key-buf (.get key-buf-field nil)
        impl-field (doto (.getDeclaredField Keyboard "implementation")
                        (.setAccessible true))
        impl (proxy [InputImplementation] []
               (pollKeyboard [bb])
               (readKeyboard [bb]))]
    (.addKeyListener
      window
      (reify KeyListener
        (keyReleased [this e]
          (.set impl-field nil impl)
          (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 0))
          (-> game
              .getInput
              .getInputProcessor
              (.keyUp (awt->libgdx (.getKeyCode e)))
              play-clj/on-gl))
        (keyTyped [this e]
          (-> game
              .getInput
              .getInputProcessor
              (.keyTyped (.getKeyChar e))
              play-clj/on-gl))
        (keyPressed [this e]
          (.set impl-field nil impl)
          (.put key-buf (awt->lwjgl (.getKeyCode e)) (byte 8))
          (-> game
              .getInput
              .getInputProcessor
              (.keyDown (awt->libgdx (.getKeyCode e)))
              play-clj/on-gl))))))