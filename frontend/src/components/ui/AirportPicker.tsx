"use client";

import { useState, useRef, useEffect, useCallback } from "react";
import { useQuery } from "@tanstack/react-query";
import { searchAirports } from "@/lib/api/airports";
import type { AirportInfo } from "@/types/api";

interface AirportPickerProps {
  label: string;
  value: AirportInfo | null;
  onChange: (airport: AirportInfo | null) => void;
  placeholder?: string;
  id?: string;
}

export function AirportPicker({
  label,
  value,
  onChange,
  placeholder = "도시 또는 공항 검색",
  id,
}: AirportPickerProps) {
  const [query, setQuery] = useState("");
  const [open, setOpen] = useState(false);
  const [highlightIdx, setHighlightIdx] = useState(-1);
  const wrapperRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const listRef = useRef<HTMLUListElement>(null);

  const { data: results = [] } = useQuery({
    queryKey: ["airports", query],
    queryFn: () => searchAirports(query),
    enabled: query.length >= 1,
    staleTime: 60_000,
  });

  // 외부 클릭 시 닫기
  useEffect(() => {
    function handleClick(e: MouseEvent) {
      if (wrapperRef.current && !wrapperRef.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClick);
    return () => document.removeEventListener("mousedown", handleClick);
  }, []);

  // 하이라이트 스크롤
  useEffect(() => {
    if (highlightIdx >= 0 && listRef.current) {
      const el = listRef.current.children[highlightIdx] as HTMLElement;
      el?.scrollIntoView({ block: "nearest" });
    }
  }, [highlightIdx]);

  const displayValue = value ? `${value.city} (${value.iata})` : "";

  function handleInputChange(text: string) {
    setQuery(text);
    setOpen(true);
    setHighlightIdx(-1);
    if (!text) {
      onChange(null);
    }
  }

  function handleSelect(airport: AirportInfo) {
    onChange(airport);
    setQuery("");
    setOpen(false);
    inputRef.current?.blur();
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (!open || results.length === 0) return;

    if (e.key === "ArrowDown") {
      e.preventDefault();
      setHighlightIdx((i) => Math.min(i + 1, results.length - 1));
    } else if (e.key === "ArrowUp") {
      e.preventDefault();
      setHighlightIdx((i) => Math.max(i - 1, 0));
    } else if (e.key === "Enter") {
      e.preventDefault();
      if (highlightIdx >= 0 && highlightIdx < results.length) {
        handleSelect(results[highlightIdx]);
      }
    } else if (e.key === "Escape") {
      setOpen(false);
    }
  }

  function handleFocus() {
    if (value) {
      // 선택된 상태에서 포커스하면 다시 검색 가능하게
      setQuery(value.city);
      setOpen(true);
    } else if (query) {
      setOpen(true);
    }
  }

  function handleClear() {
    onChange(null);
    setQuery("");
    setOpen(false);
    inputRef.current?.focus();
  }

  return (
    <div ref={wrapperRef} className="relative">
      <label
        htmlFor={id}
        className="mb-1.5 block text-sm font-medium text-[var(--color-text-secondary)]"
      >
        {label}
      </label>

      <div className="relative">
        <input
          ref={inputRef}
          id={id}
          type="text"
          value={open ? query : displayValue}
          onChange={(e) => handleInputChange(e.target.value)}
          onFocus={handleFocus}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          className="form-input pr-8"
          autoComplete="off"
        />
        {value && !open && (
          <button
            type="button"
            onClick={handleClear}
            className="absolute right-2 top-1/2 -translate-y-1/2 rounded p-1 text-[var(--color-text-tertiary)] hover:text-[var(--color-text)]"
            aria-label="선택 해제"
          >
            <svg width="14" height="14" viewBox="0 0 14 14" fill="none">
              <path d="M3.5 3.5L10.5 10.5M10.5 3.5L3.5 10.5" stroke="currentColor" strokeWidth="1.5" strokeLinecap="round" />
            </svg>
          </button>
        )}
      </div>

      {/* Dropdown */}
      {open && results.length > 0 && (
        <ul
          ref={listRef}
          className="absolute z-30 mt-1 max-h-60 w-full overflow-y-auto rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] shadow-[var(--shadow-md)]"
          role="listbox"
        >
          {results.slice(0, 20).map((airport, idx) => (
            <li
              key={airport.iata}
              role="option"
              aria-selected={highlightIdx === idx}
              onMouseDown={() => handleSelect(airport)}
              onMouseEnter={() => setHighlightIdx(idx)}
              className={`flex cursor-pointer items-center gap-3 px-3 py-2.5 text-sm transition-colors ${
                highlightIdx === idx
                  ? "bg-[var(--color-primary-soft)]"
                  : "hover:bg-[var(--color-surface-muted)]"
              } ${value?.iata === airport.iata ? "font-semibold" : ""}`}
            >
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded bg-[var(--color-surface-muted)] text-[10px] font-bold text-[var(--color-text-secondary)]">
                {airport.iata}
              </span>
              <div className="min-w-0 flex-1">
                <p className="truncate font-medium">
                  {airport.city}{" "}
                  <span className="font-normal text-[var(--color-text-tertiary)]">
                    {airport.country}
                  </span>
                </p>
                <p className="truncate text-xs text-[var(--color-text-tertiary)]">
                  {airport.name}
                </p>
              </div>
            </li>
          ))}
        </ul>
      )}

      {open && query.length >= 1 && results.length === 0 && (
        <div className="absolute z-30 mt-1 w-full rounded-[var(--radius-sm)] border border-[var(--color-border)] bg-[var(--color-surface)] p-4 text-center text-sm text-[var(--color-text-tertiary)] shadow-[var(--shadow-md)]">
          검색 결과가 없어요
        </div>
      )}
    </div>
  );
}
